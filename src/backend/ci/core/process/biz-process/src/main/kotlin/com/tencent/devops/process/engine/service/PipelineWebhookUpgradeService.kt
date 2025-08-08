/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.concurrent.Executors

@Service
class PipelineWebhookUpgradeService(
    private val scmProxyService: ScmProxyService,
    private val dslContext: DSLContext,
    private val pipelineWebhookDao: PipelineWebhookDao,
    private val client: Client,
    private val pipelineWebhookService: PipelineWebhookService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineWebhookUpgradeService::class.java)
    }

    fun updateProjectNameAndTaskId() {
        ScmType.values().forEach {
            doUpdateProjectNameAndTaskId(it)
        }
    }

    /**
     * 批量更新TASK_ID和PROJECT_NAME
     */
    @Suppress("NestedBlockDepth")
    private fun doUpdateProjectNameAndTaskId(type: ScmType) {
        val pipelines = mutableMapOf<String/*pipelineId*/, List<Element>/*trigger element*/>()
        val pipelineVariables = HashMap<String, Map<String, String>>()
        val usedTask = mutableListOf<String/*pipelineId_taskId*/>()
        var start = 0
        loop@ while (true) {
            val typeWebhooksResp = listRepositoryTypeWebhooks(type, start, 100)
            if (typeWebhooksResp.isNotOk() || typeWebhooksResp.data == null || typeWebhooksResp.data!!.isEmpty()) {
                break@loop
            }
            typeWebhooksResp.data!!.forEach webhook@{
                with(it) {
                    try {
                        val (elements, params) = getElementsAndParams(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            pipelines = pipelines,
                            pipelineVariables = pipelineVariables
                        )

                        val result = matchElement(elements = elements, params = params, usedTask = usedTask)
                        if (!result) {
                            logger.warn("$id|$pipelineId|$taskId|not match element, delete webhook $it")
                            pipelineWebhookDao.deleteById(dslContext = dslContext, projectId = projectId, id = id!!)
                        }
                    } catch (t: Throwable) {
                        logger.warn("update projectName and taskId $it exception ignore", t)
                    }
                }
            }
            start += 100
        }
    }

    fun listRepositoryTypeWebhooks(
        repositoryType: ScmType,
        start: Int,
        limit: Int
    ): Result<Collection<PipelineWebhook>> {
        if (start < 0) {
            return Result(emptyList())
        }
        val list = pipelineWebhookDao.getPipelineWebHooksByRepositoryType(dslContext, repositoryType.name, start, limit)
        val pipelineWebhookList = mutableListOf<PipelineWebhook>()
        list.forEach {
            pipelineWebhookList.add(pipelineWebhookDao.convert(it))
        }
        return Result(pipelineWebhookList)
    }

    private fun getElementsAndParams(
        projectId: String,
        pipelineId: String,
        pipelines: MutableMap<String/*pipelineId*/, List<Element>/*trigger element*/>,
        pipelineVariables: MutableMap<String, Map<String, String>>
    ): Pair<List<Element>, Map<String, String>> {
        return if (pipelines[pipelineId] == null) {
            val model = pipelineWebhookService.getModel(projectId, pipelineId)
            // 如果model为空,缓存空值
            val (elements, params) = if (model == null) {
                Pair(emptyList(), emptyMap())
            } else {
                val triggerContainer = model.getTriggerContainer()
                val params = triggerContainer.params.associate { param ->
                    param.id to param.defaultValue.toString()
                }
                Pair(triggerContainer.elements.filterIsInstance<WebHookTriggerElement>(), params)
            }
            pipelines[pipelineId] = elements
            pipelineVariables[pipelineId] = params
            Pair(elements, params)
        } else {
            Pair(pipelines[pipelineId]!!, pipelineVariables[pipelineId]!!)
        }
    }

    @Suppress("LoopWithTooManyJumpStatements", "ComplexMethod")
    private fun PipelineWebhook.matchElement(
        elements: List<Element>,
        params: Map<String, String>,
        usedTask: MutableList<String>
    ): Boolean {
        val webhookRepositoryConfig = RepositoryConfigUtils.getRepositoryConfig(
            repoHashId = repoHashId,
            repoName = repoName,
            repoType = repoType,
            variables = params
        )
        if (elements.isEmpty()) {
            logger.warn("$id|$pipelineId|$taskId|pipeline does not exist")
            return false
        }
        val repo = try {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = webhookRepositoryConfig.getURLEncodeRepositoryId(),
                repositoryType = webhookRepositoryConfig.repositoryType
            ).data
        } catch (e: Exception) {
            null
        }
        if (repo == null) {
            logger.warn("$id|$pipelineId|$taskId|repo[$webhookRepositoryConfig] does not exist")
            return false
        }
        var findResult = false
        for (element in elements) {
            val (elementScmType, elementEventType, elementRepositoryConfig) =
                RepositoryConfigUtils.buildWebhookConfig(element, params)
            val usedKey = "${pipelineId}_${element.id!!}"
            if (webhookRepositoryConfig.getRepositoryId() == elementRepositoryConfig.getRepositoryId() &&
                elementScmType == repositoryType &&
                !usedTask.contains(usedKey)
            ) {
                /*
                * 配置相同并且没有使用过才进行更新和标记
                * 1. 如果taskId为空,则表示没有更新过，直接更新
                * 2. 如果taskId不为空,taskId和插件ID相同,则标记已使用
                * */
                if (taskId == null) {
                    pipelineWebhookDao.updateProjectNameAndTaskId(
                        dslContext = dslContext,
                        projectId = projectId,
                        projectName = pipelineWebhookService.getProjectName(repo.projectName),
                        taskId = element.id!!,
                        id = id!!
                    )
                    usedTask.add(usedKey)
                    findResult = true
                    break
                } else if (taskId == element.id) {
                    usedTask.add(usedKey)
                    findResult = true
                    break
                }
            }
        }
        return findResult
    }

    fun updateWebhookSecret(type: ScmType) {
        val pipelines = mutableMapOf<String/*pipelineId*/, List<Element>/*trigger element*/>()
        val pipelineVariables = HashMap<String, Map<String, String>>()
        var start = 0
        loop@ while (true) {
            logger.info("update webhook secret|start=$start")
            val typeWebhooksResp = listRepositoryTypeWebhooks(type, start, 100)
            if (typeWebhooksResp.isNotOk() || typeWebhooksResp.data == null || typeWebhooksResp.data!!.isEmpty()) {
                break@loop
            }
            typeWebhooksResp.data!!.forEach webhook@{
                it.doUpdateWebhookSecret(pipelines, pipelineVariables)
            }
            start += 100
        }
    }

    @Suppress("NestedBlockDepth")
    private fun PipelineWebhook.doUpdateWebhookSecret(
        pipelines: MutableMap<String, List<Element>>,
        pipelineVariables: HashMap<String, Map<String, String>>
    ) {
        try {
            val (elements, params) = getElementsAndParams(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelines = pipelines,
                pipelineVariables = pipelineVariables
            )

            val repositoryConfig = RepositoryConfigUtils.getRepositoryConfig(
                repoHashId = repoHashId,
                repoName = repoName,
                repoType = repoType,
                variables = params
            )
            elements.forEach { element ->
                if (element.id == taskId) {
                    when (element) {
                        is CodeGitWebHookTriggerElement ->
                            scmProxyService.addGitWebhook(
                                projectId,
                                repositoryConfig = repositoryConfig,
                                codeEventType = element.eventType
                            )

                        is CodeTGitWebHookTriggerElement ->
                            scmProxyService.addTGitWebhook(
                                projectId,
                                repositoryConfig = repositoryConfig,
                                codeEventType = element.data.input.eventType
                            )

                        is CodeGitlabWebHookTriggerElement ->
                            scmProxyService.addGitlabWebhook(
                                projectId,
                                repositoryConfig = repositoryConfig,
                                codeEventType = element.eventType
                            )
                    }
                    return
                }
            }
        } catch (t: Throwable) {
            logger.warn("$id|$pipelineId|update webhook secret exception ignore", t)
        }
    }

    fun updateWebhookEventInfo(
        projectId: String?
    ) {
        val startTime = System.currentTimeMillis()
        val threadPoolExecutor = Executors.newSingleThreadExecutor()
        threadPoolExecutor.submit {
            logger.info("PipelineWebhookService:begin updateWebhookEventInfo threadPoolExecutor")
            try {
                updateWebhookEventInfoTask(projectId = projectId)
            } catch (ignored: Exception) {
                logger.warn("PipelineWebhookService：updateWebhookEventInfo failed", ignored)
            } finally {
                threadPoolExecutor.shutdown()
                logger.info("updateWebhookEventInfo finish cost: ${System.currentTimeMillis() - startTime}")
            }
        }
    }

    fun updateWebhookProjectName(projectId: String?, pipelineId: String?) {
        ThreadPoolUtil.submitAction(
            actionTitle = "updateWebhookProjectName",
            action = {
                updateProjectName(projectId, pipelineId)
            }
        )
    }

    private fun updateProjectName(projectId: String?, pipelineId: String?) {
        logger.info("start update webhook project name|projectId:$projectId")
        var offset = 0
        val limit = 100
        var webhookList = listOf<PipelineWebhook>()
        val repoCache = mutableMapOf<String, Repository?>()
        do {
            webhookList = pipelineWebhookDao.listWebhook(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                ignoredRepoTypes = setOf(ScmType.CODE_SVN.name, ScmType.CODE_P4.name),
                limit = limit,
                offset = offset
            ) ?: mutableListOf()
            // 仅查没缓存的仓库
            val repoIds = webhookList.mapNotNull { it.repositoryHashId }.filter { !repoCache.containsKey(it) }.toSet()
            val repositoryList = try {
                client.get(ServiceRepositoryResource::class).listRepoByIds(
                    repositoryIds = repoIds
                ).data
            } catch (ignored: Exception) {
                logger.warn("failed to get repository list", ignored)
                null
            } ?: listOf()
            // 仓库信息
            repositoryList.filter { !it.repoHashId.isNullOrBlank() }.forEach {
                repoCache[it.repoHashId!!] = it
            }
            webhookList.filter { !it.repositoryHashId.isNullOrBlank() }.forEach { webhook ->
                val repository = repoCache[webhook.repositoryHashId]
                if (repository == null) {
                    logger.info("${webhook.projectId}|repoCache[${webhook.repositoryHashId}] is null")
                    return@forEach
                }
                if (webhook.projectName != repository.projectName) {
                    val count = pipelineWebhookDao.updateProjectName(
                        dslContext = dslContext,
                        projectId = webhook.projectId,
                        pipelineId = webhook.pipelineId,
                        taskId = webhook.taskId!!,
                        projectName = repository.projectName
                    )
                    logger.info(
                        "${webhook.projectId}|${webhook.pipelineId}|${webhook.taskId}|update webhook projectName|" +
                                "[${webhook.projectName}]==>[${repository.projectName}]|changeCount[$count]"
                    )
                }
            }
            offset += limit
        } while (webhookList.size == 100)
        logger.info("final update webhook project name|projectId:$projectId")
    }

    private fun updateWebhookEventInfoTask(projectId: String?) {
        var offset = 0
        val limit = 1000
        val repoCache = mutableMapOf<String, Optional<Repository>>()
        // 上一个更新的项目ID
        var preProjectId: String? = null
        do {
            val pipelines = pipelineWebhookDao.groupPipelineList(
                dslContext = dslContext,
                projectId = projectId,
                limit = limit,
                offset = offset
            )
            pipelines.forEach { (projectId, pipelineId) ->
                // 更改项目,清空代码库缓存
                if (preProjectId != null && preProjectId != projectId) {
                    repoCache.clear()
                }
                preProjectId = projectId
                updatePipelineEventInfo(projectId = projectId, pipelineId = pipelineId, repoCache = repoCache)
            }
            offset += limit
        } while (pipelines.size == 1000)
    }

    @Suppress("CyclomaticComplexMethod", "ComplexMethod")
    private fun updatePipelineEventInfo(
        projectId: String,
        pipelineId: String,
        repoCache: MutableMap<String, Optional<Repository>>
    ) {
        val model = pipelineWebhookService.getModel(projectId, pipelineId)
        if (model == null) {
            logger.info("$projectId|$pipelineId|model is null")
            return
        }
        val triggerContainer = model.getTriggerContainer()
        val params = PipelineVarUtil.fillVariableMap(
            triggerContainer.params.associate { param ->
                param.id to param.defaultValue.toString()
            }
        )
        val elementMap =
            triggerContainer.elements.filterIsInstance<WebHookTriggerElement>().associateBy { it.id }
        val pipelineWebhooks = pipelineWebhookDao.listWebhook(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            limit = 1000,
            offset = 0
        )
        pipelineWebhooks?.forEach webhook@{ webhook ->
            try {
                if (webhook.taskId.isNullOrBlank()) return@webhook
                val element = elementMap[webhook.taskId] ?: return@webhook
                val (elementScmType, elementEventType, elementRepositoryConfig) =
                    RepositoryConfigUtils.buildWebhookConfig(element, params)
                val webhookRepositoryConfig = RepositoryConfigUtils.getRepositoryConfig(
                    repoHashId = webhook.repoHashId,
                    repoName = webhook.repoName,
                    repoType = webhook.repoType,
                    variables = params
                )
                // 插件的配置与表中数据不一致,如保存流水线时,注册webhook失败,就会导致数据不一致,打印日志统计
                if (elementRepositoryConfig.getRepositoryId() != webhookRepositoryConfig.getRepositoryId()) {
                    logger.info(
                        "webhook repository config different from element repository config|" +
                                "webhook:$webhookRepositoryConfig|element:$elementRepositoryConfig"
                    )
                }
                val repository = getAndCacheRepo(projectId, webhookRepositoryConfig, repoCache)

                // 历史原因,假如git的projectName有三个，如aaa/bbb/ccc,只读取了bbb,导致触发时获取的流水线数量很多,记录日志
                if (repository != null && webhook.projectName != repository.projectName) {
                    logger.info(
                        "webhook projectName different from repo projectName|$projectId|$pipelineId|" +
                                "webhook:${webhook.projectName}|repo:${repository.projectName}"
                    )
                }
                val repositoryHashId = when {
                    repository != null -> repository.repoHashId
                    webhookRepositoryConfig.repositoryType == RepositoryType.ID ->
                        webhookRepositoryConfig.repositoryHashId

                    else -> null
                }
                val externalName = when {
                    repository != null -> pipelineWebhookService.getExternalName(
                        scmType = repository.getScmType(),
                        projectName = repository.projectName
                    )

                    else -> webhook.projectName
                }
                pipelineWebhookDao.updateWebhookEventInfo(
                    dslContext = dslContext,
                    eventType = elementEventType?.name ?: "",
                    externalId = repository?.getExternalId(),
                    externalName = externalName,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = webhook.taskId!!,
                    repositoryHashId = repositoryHashId
                )
            } catch (ignored: Exception) {
                logger.info("update webhook event info error|$webhook", ignored)
            }
        }
    }

    private fun getAndCacheRepo(
        projectId: String,
        webhookRepositoryConfig: RepositoryConfig,
        repoCache: MutableMap<String, Optional<Repository>>
    ): Repository? {
        // 缓存代码库信息,避免频繁调用代码库信息接口
        val repoCacheKey = "${projectId}_${webhookRepositoryConfig.getRepositoryId()}"
        val repositoryOptional = repoCache[repoCacheKey] ?: run {
            val repo = try {
                scmProxyService.getRepo(
                    projectId = projectId,
                    repositoryConfig = webhookRepositoryConfig
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "$projectId|${webhookRepositoryConfig.getRepositoryId()}|" +
                            "fail to get repository info", ignored
                )
                null
            }
            val optional = Optional.ofNullable(repo)
            repoCache[repoCacheKey] = optional
            optional
        }
        val repository = if (repositoryOptional.isPresent) {
            repositoryOptional.get()
        } else {
            null
        }
        return repository
    }
}
