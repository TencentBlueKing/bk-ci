/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.engine.pojo.PipelineWebhook
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

/**
 * 流水线webhook存储服务
 * @version 1.0
 */
@Service
class PipelineWebhookService @Autowired constructor(
    private val scmProxyService: ScmProxyService,
    private val dslContext: DSLContext,
    private val pipelineWebhookDao: PipelineWebhookDao,
    private val pipelineResDao: PipelineResDao,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineInfoDao: PipelineInfoDao
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!
    private val executor = Executors.newFixedThreadPool(5)

    fun saveWebhook(
        pipelineWebhook: PipelineWebhook,
        codeEventType: CodeEventType? = null,
        variables: Map<String, String>? = null,
        createPipelineFlag: Boolean? = false
    ): Result<Boolean> {
        logger.info("save Webhook[$pipelineWebhook]")
        val repositoryConfig = getRepositoryConfig(pipelineWebhook, variables)

        var continueFlag = true
        if (createPipelineFlag != null && createPipelineFlag) {
            // 新增流水线时，模版里配置的代码库是变量或者当前项目下不存在，不需创建webhook
            try {
                scmProxyService.getRepo(pipelineWebhook.projectId, repositoryConfig)
            } catch (e: Exception) {
                logger.info("skip save Webhook[$pipelineWebhook]: ${e.message}")
                continueFlag = false
            }
        }

        if (continueFlag) {
            val projectName = when (pipelineWebhook.repositoryType) {
                ScmType.CODE_GIT ->
                    scmProxyService.addGitWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
                ScmType.CODE_SVN ->
                    scmProxyService.addSvnWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.CODE_GITLAB ->
                    scmProxyService.addGitlabWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.GITHUB -> {
                    val repo = client.get(ServiceRepositoryResource::class).get(
                        pipelineWebhook.projectId,
                        repositoryConfig.getURLEncodeRepositoryId(),
                        repositoryConfig.repositoryType
                    ).data!!
                    repo.projectName
                }
                ScmType.CODE_TGIT -> {
                    scmProxyService.addTGitWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
                }
                else -> {
                    null
                }
            }
            logger.info("add $projectName webhook to [$pipelineWebhook]")
            if (!projectName.isNullOrBlank()) {
                pipelineWebhook.projectName = getProjectName(projectName!!)
                pipelineWebhookDao.save(
                    dslContext = dslContext,
                    pipelineWebhook = pipelineWebhook
                )
            }
        }
        return Result(true)
    }

    fun saveWebhook(
        pipelineWebhook: PipelineWebhook,
        repo: Repository,
        codeEventType: CodeEventType? = null,
        hookUrl: String? = null,
        token: String? = null
    ) {
        logger.info("save generic Webhook[$pipelineWebhook]")
        scmProxyService.addGenericWebhook(
            projectId = pipelineWebhook.projectId,
            repo = repo,
            scmType = pipelineWebhook.repositoryType,
            codeEventType = codeEventType,
            hookUrl = hookUrl,
            token = token
        )
        pipelineWebhookDao.save(
            dslContext = dslContext,
            pipelineWebhook = pipelineWebhook
        )
    }

    fun deleteWebhook(pipelineId: String, userId: String): Result<Boolean> {
        logger.info("delete $pipelineId webhook by $userId")
        pipelineWebhookDao.delete(dslContext, pipelineId)
        return Result(true)
    }

    fun deleteWebhook(
        pipelineId: String,
        taskId: String,
        userId: String
    ): Result<Boolean> {
        logger.info("delete pipelineId:$pipelineId, taskId:$taskId webhook by $userId")
        pipelineWebhookDao.delete(dslContext, pipelineId, taskId)
        return Result(true)
    }

    fun getModel(pipelineId: String, version: Int? = null): Model? {
        val modelString = pipelineResDao.getVersionModelString(dslContext, pipelineId, version) ?: return null
        return try {
            objectMapper.readValue(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.error("get process($pipelineId) model fail", e)
            null
        }
    }

    fun getWebhookPipelines(name: String, type: String): Set<String> {
        return pipelineWebhookDao.getByProjectNameAndType(
            dslContext = dslContext,
            projectName = getProjectName(name),
            repositoryType = getWebhookScmType(type).name
        )?.map { it.pipelineId }?.toSet() ?: setOf()
    }

    fun getWebhookScmType(type: String) =
        when (type) {
            CodeGitWebHookTriggerElement.classType -> {
                ScmType.CODE_GIT
            }
            CodeSVNWebHookTriggerElement.classType -> {
                ScmType.CODE_SVN
            }
            CodeGitlabWebHookTriggerElement.classType -> {
                ScmType.CODE_GITLAB
            }
            CodeGithubWebHookTriggerElement.classType -> {
                ScmType.GITHUB
            }
            CodeTGitWebHookTriggerElement.classType -> {
                ScmType.CODE_TGIT
            }
            else -> {
                throw RuntimeException("Unknown web hook type($type)")
            }
        }

    fun getProjectName(projectName: String): String {
        // 如果项目名是三层的，比如a/b/c，那对应的rep_name是b
        val repoSplit = projectName.split("/")
        if (repoSplit.size != 3) {
            return projectName
        }
        return repoSplit[1].trim()
    }

    private fun getRepositoryConfig(
        pipelineWebhook: PipelineWebhook,
        variable: Map<String, String>? = null
    ): RepositoryConfig {
        return getRepositoryConfig(
            repoHashId = pipelineWebhook.repoHashId,
            repoName = pipelineWebhook.repoName,
            repoType = pipelineWebhook.repoType,
            variable = variable
        )
    }

    private fun getRepositoryConfig(
        repoHashId: String?,
        repoName: String?,
        repoType: RepositoryType?,
        variable: Map<String, String>? = null
    ): RepositoryConfig {
        return when (repoType) {
            RepositoryType.ID -> RepositoryConfig(repoHashId, null, RepositoryType.ID)
            RepositoryType.NAME -> {
                val repositoryName = if (variable == null || variable.isEmpty()) {
                    repoName!!
                } else {
                    EnvUtils.parseEnv(repoName!!, variable)
                }
                RepositoryConfig(null, repositoryName, RepositoryType.NAME)
            }
            else -> {
                if (!repoHashId.isNullOrBlank()) {
                    RepositoryConfig(repoHashId, null, RepositoryType.ID)
                } else if (!repoName.isNullOrBlank()) {
                    val repositoryName = if (variable == null || variable.isEmpty()) {
                        repoName!!
                    } else {
                        EnvUtils.parseEnv(repoName!!, variable)
                    }
                    RepositoryConfig(null, repositoryName, RepositoryType.NAME)
                } else {
                    // 两者不能同时为空
                    throw ErrorCodeException(
                        defaultMessage = "Webhook 的ID和名称同时为空",
                        errorCode = ProcessMessageCode.ERROR_PARAM_WEBHOOK_ID_NAME_ALL_NULL
                    )
                }
            }
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

    fun updateProjectNameAndTaskId() {
        ScmType.values().forEach {
            executor.execute {
                doUpdateProjectNameAndTaskId(it)
            }
        }
    }

    /**
     * 批量更新TASK_ID和PROJECT_NAME
     */
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
                            pipelineId = pipelineId,
                            pipelines = pipelines,
                            pipelineVariables = pipelineVariables
                        )

                        val result = matchElement(elements = elements, params = params, usedTask = usedTask)
                        if (!result) {
                            logger.warn("$id|$pipelineId|$taskId|not match element, delete webhook $it")
                            pipelineWebhookDao.deleteById(dslContext = dslContext, id = id!!)
                        }
                    } catch (t: Throwable) {
                        logger.warn("update projectName and taskId $it exception ignore", t)
                    }
                }
            }
            start += 100
        }
    }

    private fun getElementsAndParams(
        pipelineId: String,
        pipelines: MutableMap<String/*pipelineId*/, List<Element>/*trigger element*/>,
        pipelineVariables: MutableMap<String, Map<String, String>>
    ): Pair<List<Element>, Map<String, String>> {
        return if (pipelines[pipelineId] == null) {
            val model = getModel(pipelineId)
            // 如果model为空,缓存空值
            val (elements, params) = if (model == null) {
                Pair(emptyList(), emptyMap())
            } else {
                val triggerContainer = model.stages[0].containers[0] as TriggerContainer
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

    private fun PipelineWebhook.matchElement(
        elements: List<Element>,
        params: Map<String, String>,
        usedTask: MutableList<String>
    ): Boolean {
        val webhookRepositoryConfig = getRepositoryConfig(this, params)
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
            val elementRepositoryConfig = getElementRepositoryConfig(element, params) ?: continue
            val usedKey = "${pipelineId}_${element.id!!}"
            if (webhookRepositoryConfig.getRepositoryId() == elementRepositoryConfig.getRepositoryId() &&
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
                        projectName = getProjectName(repo.projectName),
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

    private fun getElementRepositoryConfig(
        element: Element,
        variable: Map<String, String>
    ): RepositoryConfig? {
        if (element !is WebHookTriggerElement) {
            return null
        }
        val repositoryConfig = RepositoryConfigUtils.buildConfig(element)
        return with(repositoryConfig) {
            getRepositoryConfig(
                repoHashId = repositoryHashId,
                repoName = repositoryName,
                repoType = repositoryType,
                variable = variable
            )
        }
    }

    // TODO 这段代码在灰度验证后要删除
    fun reverseComparison() {
        listOf(
            "codeGitWebHookTrigger",
            "codeGithubWebHookTrigger",
            "codeGitlabWebHookTrigger",
            "codeSVNWebHookTrigger",
            "codeTGitWebHookTrigger"
        ).forEach {
            executor.execute { doReverseComparison(it) }
        }
    }

    private fun doReverseComparison(
        atomCode: String
    ) {
        val pipelines = mutableMapOf<String/*pipelineId*/, List<Element>/*trigger element*/>()
        val pipelineVariables = HashMap<String, Map<String, String>>()
        var start = 0
        loop@ while (true) {
            val pipelineIds = pipelineModelTaskDao.getPipelineIdsByAtomCode(
                dslContext = dslContext,
                atomCode = atomCode,
                offset = start,
                limit = 100
            )
            if (pipelineIds == null || pipelineIds.isEmpty()) {
                break@loop
            }
            pipelineIds.forEach pipelineId@{ pipelineId ->
                val pipelineInfo = pipelineInfoDao.getPipelineInfo(dslContext = dslContext, pipelineId = pipelineId)
                    ?: return@pipelineId
                val (elements, params) = getElementsAndParams(
                    pipelineId = pipelineId,
                    pipelines = pipelines,
                    pipelineVariables = pipelineVariables
                )
                val webhooks = pipelineWebhookDao.listWebhookByPipelineId(
                    dslContext = dslContext,
                    pipelineId = pipelineId
                )
                for (element in elements) {
                    if (!element.matchWebhook(
                            projectId = pipelineInfo.projectId,
                            webhooks = webhooks,
                            params = params
                        )
                    ) {
                        saveNotMatchElement(
                            projectId = pipelineInfo.projectId,
                            pipelineId = pipelineId,
                            element = element,
                            params = params
                        )
                    }
                }
            }
            start += 100
        }
    }

    private fun Element.matchWebhook(
        projectId: String,
        webhooks: List<PipelineWebhook>,
        params: Map<String, String>
    ): Boolean {
        val elementRepositoryConfig = getElementRepositoryConfig(this, params) ?: return true
        // 先匹配webhook,因为webhook匹配上的概率要大很多
        webhooks.forEach { webhook ->
            val webhookRepositoryConfig = getRepositoryConfig(pipelineWebhook = webhook, variable = params)
            if (webhookRepositoryConfig.getRepositoryId() == elementRepositoryConfig.getRepositoryId()) {
                return true
            }
        }
        // 如果webhook匹配不上,再查看仓库是否存在，如果不存在就不需要去保存webhook
        val repo = try {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = elementRepositoryConfig.getURLEncodeRepositoryId(),
                repositoryType = elementRepositoryConfig.repositoryType
            ).data
        } catch (e: Exception) {
            null
        }
        if (repo == null) {
            logger.info("$projectId|$id|$elementRepositoryConfig| repo not found")
            return true
        }
        return false
    }

    private fun saveNotMatchElement(
        projectId: String,
        pipelineId: String,
        element: Element,
        params: Map<String, String>
    ) {
        val (repositoryConfig, scmType, eventType) = when (element) {
            is CodeGitWebHookTriggerElement -> Triple(
                RepositoryConfigUtils.buildConfig(element),
                ScmType.CODE_GIT,
                element.eventType
            )
            is CodeGitlabWebHookTriggerElement -> Triple(
                RepositoryConfigUtils.buildConfig(element),
                ScmType.CODE_GITLAB,
                null
            )
            is CodeSVNWebHookTriggerElement -> Triple(
                RepositoryConfigUtils.buildConfig(element),
                ScmType.CODE_SVN,
                null
            )
            is CodeGithubWebHookTriggerElement -> Triple(
                RepositoryConfigUtils.buildConfig(element),
                ScmType.GITHUB,
                null
            )
            is CodeTGitWebHookTriggerElement -> Triple(
                RepositoryConfigUtils.buildConfig(element),
                ScmType.CODE_TGIT,
                element.data.input.eventType
            )
            else -> Triple(null, null, null)
        }
        if (repositoryConfig != null && scmType != null) {
            try {
                saveWebhook(
                    pipelineWebhook = PipelineWebhook(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        repositoryType = scmType,
                        repoType = repositoryConfig.repositoryType,
                        repoHashId = repositoryConfig.repositoryHashId,
                        repoName = repositoryConfig.repositoryName,
                        taskId = element.id
                    ), codeEventType = eventType, variables = params,
                    createPipelineFlag = true
                )
                logger.info("$projectId|$pipelineId|${element.id}|$repositoryConfig|save not match element success")
            } catch (e: Throwable) {
                logger.error("$projectId|$pipelineId|${element.id}|$repositoryConfig|save not match element fail", e)
            }
        }
    }
}
