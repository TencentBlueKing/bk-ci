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
 */

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.engine.pojo.WebhookElementParams
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线webhook存储服务
 * @version 1.0
 */
@Suppress("ALL")
@Service
class PipelineWebhookService @Autowired constructor(
    private val scmProxyService: ScmProxyService,
    private val dslContext: DSLContext,
    private val pipelineWebhookDao: PipelineWebhookDao,
    private val pipelineResDao: PipelineResDao,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val pipelinePermissionService: PipelinePermissionService,
    private val redisOperation: RedisOperation
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun addWebhook(
        projectId: String,
        pipelineId: String,
        version: Int?,
        userId: String
    ) {
        val model = getModel(projectId, pipelineId, version)
        if (model == null) {
            logger.info("$pipelineId|$version|model is null")
            return
        }
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val params = triggerContainer.params.associate { param ->
            param.id to param.defaultValue.toString()
        }
        val elements = triggerContainer.elements.filterIsInstance<WebHookTriggerElement>()
        val failedElementNames = mutableListOf<String>()
        elements.forEach { element ->
            val webhookElementParams = getElementRepositoryConfig(element, variable = params)
                ?: return@forEach
            with(webhookElementParams) {
                try {
                    logger.info("$pipelineId| Trying to add the $scmType web hook for repo($repositoryConfig)")
                    saveWebhook(
                        pipelineWebhook = PipelineWebhook(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            repositoryType = scmType,
                            repoType = repositoryConfig.repositoryType,
                            repoHashId = repositoryConfig.repositoryHashId,
                            repoName = repositoryConfig.repositoryName,
                            taskId = element.id
                        ),
                        codeEventType = eventType,
                        repositoryConfig = repositoryConfig,
                        createPipelineFlag = true,
                        version = element.version
                    )
                } catch (ignore: Exception) {
                    failedElementNames.add("- ${element.name}: ${ignore.message}")
                    logger.warn("$projectId|$pipelineId|add webhook failed", ignore)
                }
            }
        }
        if (failedElementNames.isNotEmpty()) {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                SendNotifyMessageTemplateRequest(
                    templateCode =
                    PipelineNotifyTemplateEnum.PIPELINE_WEBHOOK_REGISTER_FAILURE_NOTIFY_TEMPLATE.templateCode,
                    receivers = mutableSetOf(userId),
                    notifyType = mutableSetOf(NotifyType.RTX.name),
                    titleParams = mapOf("pipelineName" to model.name),
                    bodyParams = mapOf(
                        "pipelineName" to model.name,
                        "elementNames" to failedElementNames.joinToString(""),
                        "pipelineEditUrl" to pipelineEditUrl(projectId, pipelineId)
                    ),
                    cc = null,
                    bcc = null
                )
            )
        }
    }

    fun saveWebhook(
        pipelineWebhook: PipelineWebhook,
        codeEventType: CodeEventType? = null,
        repositoryConfig: RepositoryConfig,
        createPipelineFlag: Boolean? = false,
        version: String
    ) {
        logger.info("save Webhook[$pipelineWebhook]")
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
            val projectName = registerWebhook(
                pipelineWebhook = pipelineWebhook,
                repositoryConfig = repositoryConfig,
                codeEventType = codeEventType,
                version = version
            )
            logger.info("add $projectName webhook to [$pipelineWebhook]")
            if (!projectName.isNullOrBlank()) {
                pipelineWebhook.projectName = getProjectName(projectName)
                pipelineWebhookDao.save(
                    dslContext = dslContext,
                    pipelineWebhook = pipelineWebhook
                )
            }
        }
    }

    private fun registerWebhook(
        pipelineWebhook: PipelineWebhook,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?,
        version: String
    ): String? {
        // 防止同一个仓库注册多个相同事件的webhook
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "webhook:register:${repositoryConfig.getRepositoryId()}:$codeEventType",
            expiredTimeInSeconds = 30
        )
        try {
            redisLock.lock()
            return when (pipelineWebhook.repositoryType) {
                ScmType.CODE_GIT ->
                    scmProxyService.addGitWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
                ScmType.CODE_SVN ->
                    scmProxyService.addSvnWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.CODE_GITLAB ->
                    scmProxyService.addGitlabWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
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
                ScmType.CODE_P4 ->
                    if (WebhookUtils.isCustomP4TriggerVersion(version)) {
                        val repo = client.get(ServiceRepositoryResource::class).get(
                            pipelineWebhook.projectId,
                            repositoryConfig.getURLEncodeRepositoryId(),
                            repositoryConfig.repositoryType
                        ).data!!
                        repo.projectName
                    } else {
                        scmProxyService.addP4Webhook(
                            projectId = pipelineWebhook.projectId,
                            repositoryConfig = repositoryConfig,
                            codeEventType = codeEventType
                        )
                    }
                else -> {
                    null
                }
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun pipelineEditUrl(projectId: String, pipelineId: String) =
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/edit"

    fun deleteWebhook(projectId: String, pipelineId: String, userId: String): Result<Boolean> {
        logger.info("delete $pipelineId webhook by $userId")
        pipelineWebhookDao.deleteByPipelineId(dslContext, projectId, pipelineId)
        return Result(true)
    }

    fun deleteWebhook(
        projectId: String,
        pipelineId: String,
        taskId: String,
        userId: String
    ): Result<Boolean> {
        logger.info("delete pipelineId:$pipelineId, taskId:$taskId webhook by $userId")
        pipelineWebhookDao.deleteByTaskId(dslContext, projectId, pipelineId, taskId)
        return Result(true)
    }

    fun getModel(projectId: String, pipelineId: String, version: Int? = null): Model? {
        val modelString =
            pipelineResDao.getVersionModelString(dslContext, projectId, pipelineId, version) ?: return null
        return try {
            objectMapper.readValue(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.warn("get process($pipelineId) model fail", e)
            null
        }
    }

    fun getWebhookPipelines(name: String, type: String): Set<Pair<String, String>> {
        val records = pipelineWebhookDao.getByProjectNameAndType(
            dslContext = dslContext,
            projectName = getProjectName(name),
            repositoryType = getWebhookScmType(type).name
        )
        val pipelineWebhookSet = mutableSetOf<Pair<String, String>>()
        records?.forEach {
            pipelineWebhookSet.add(Pair(it.value1(), it.value2()))
        }
        return pipelineWebhookSet
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
            CodeP4WebHookTriggerElement.classType -> {
                ScmType.CODE_P4
            }
            else -> {
                throw IllegalArgumentException("Unknown web hook type($type)")
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
                        repoName
                    } else {
                        EnvUtils.parseEnv(repoName, variable)
                    }
                    RepositoryConfig(null, repositoryName, RepositoryType.NAME)
                } else {
                    // 两者不能同时为空
                    throw ErrorCodeException(
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
            doUpdateProjectNameAndTaskId(it)
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

    private fun getElementsAndParams(
        projectId: String,
        pipelineId: String,
        pipelines: MutableMap<String/*pipelineId*/, List<Element>/*trigger element*/>,
        pipelineVariables: MutableMap<String, Map<String, String>>
    ): Pair<List<Element>, Map<String, String>> {
        return if (pipelines[pipelineId] == null) {
            val model = getModel(projectId, pipelineId)
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
            val (elementRepositoryConfig, elementScmType) = getElementRepositoryConfig(element, params) ?: continue
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
    ): WebhookElementParams? {
        if (element !is WebHookTriggerElement) {
            return null
        }
        val elementRepositoryConfig = RepositoryConfigUtils.buildConfig(element)
        val realRepositoryConfig = with(elementRepositoryConfig) {
            getRepositoryConfig(
                repoHashId = repositoryHashId,
                repoName = repositoryName,
                repoType = repositoryType,
                variable = variable
            )
        }
        return when (element) {
            is CodeGitWebHookTriggerElement ->
                WebhookElementParams(
                    repositoryConfig = realRepositoryConfig,
                    scmType = ScmType.CODE_GIT,
                    eventType = element.eventType
                )
            is CodeGithubWebHookTriggerElement ->
                WebhookElementParams(
                    repositoryConfig = realRepositoryConfig,
                    scmType = ScmType.GITHUB,
                    eventType = null
                )
            is CodeGitlabWebHookTriggerElement ->
                WebhookElementParams(
                    repositoryConfig = realRepositoryConfig,
                    scmType = ScmType.CODE_GITLAB,
                    eventType = element.eventType
                )
            is CodeSVNWebHookTriggerElement ->
                WebhookElementParams(
                    repositoryConfig = realRepositoryConfig,
                    scmType = ScmType.CODE_SVN,
                    eventType = null
                )
            is CodeTGitWebHookTriggerElement ->
                WebhookElementParams(
                    repositoryConfig = realRepositoryConfig,
                    scmType = ScmType.CODE_TGIT,
                    eventType = element.data.input.eventType
                )
            is CodeP4WebHookTriggerElement ->
                WebhookElementParams(
                    repositoryConfig = realRepositoryConfig,
                    scmType = ScmType.CODE_P4,
                    eventType = element.data.input.eventType
                )
            else ->
                throw InvalidParamException("Unknown code element -> $element")
        }
    }

    fun listWebhook(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): List<PipelineWebhook> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
        return pipelineWebhookDao.listWebhook(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = limit.offset,
            limit = limit.limit
        ) ?: emptyList()
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

            val repositoryConfig = getRepositoryConfig(this, params)
            for (element in elements) {
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
                    break
                }
            }
        } catch (t: Throwable) {
            logger.warn("$id|$pipelineId|update webhook secret exception ignore", t)
        }
    }
}
