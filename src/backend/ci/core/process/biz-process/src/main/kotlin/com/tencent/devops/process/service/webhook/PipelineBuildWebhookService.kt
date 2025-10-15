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
 */

package com.tencent.devops.process.service.webhook

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.measure.ProjectUserDailyEvent
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsEvent
import com.tencent.devops.common.event.pojo.measure.UserOperateCounterData
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.CascadePropertyUtils
import com.tencent.devops.common.pipeline.utils.PIPELINE_PAC_REPO_HASH_ID
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebHookQueueService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.engine.service.code.GitWebhookUnlockDispatcher
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.code.WebhookBuildResult
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatch
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.service.builds.PipelineBuildCommitService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import jakarta.ws.rs.core.Response
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildWebhookService @Autowired constructor(
    private val client: Client,
    private val pipelineWebhookService: PipelineWebhookService,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildService: PipelineBuildService,
    private val gitWebhookUnlockDispatcher: GitWebhookUnlockDispatcher,
    private val pipelineWebHookQueueService: PipelineWebHookQueueService,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineBuildCommitService: PipelineBuildCommitService,
    private val webhookBuildParameterService: WebhookBuildParameterService,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val measureEventDispatcher: SampleEventDispatcher,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelinePermissionService: PipelinePermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookService::class.java)
        private const val WEBHOOK_COMMIT_TRIGGER = "webhook_commit_trigger"
    }

    fun dispatchTriggerPipelines(
        matcher: ScmWebhookMatcher,
        triggerEvent: PipelineTriggerEvent,
        triggerPipelines: List<WebhookTriggerPipeline>
    ): Boolean {
        try {
            logger.info("dispatch pipeline webhook subscriber|repo(${matcher.getRepoName()})")
            EventCacheUtil.initEventCache()
            if (triggerPipelines.isEmpty()) {
                gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
                return false
            }

            // 代码库触发的事件ID,一个代码库会触发多条流水线,但应该只有一条触发事件
            val repoEventIdMap = mutableMapOf<String, Long>()
            triggerPipelines.forEach outside@{ subscriber ->
                val projectId = subscriber.projectId
                val pipelineId = subscriber.pipelineId
                try {
                    logger.info("pipelineId is $pipelineId")
                    val builder = PipelineTriggerDetailBuilder()
                        .projectId(projectId)
                        .pipelineId(pipelineId)

                    webhookTriggerPipelineBuild(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        matcher = matcher,
                        builder = builder
                    )
                    saveTriggerEvent(
                        projectId = projectId,
                        builder = builder,
                        triggerEvent = triggerEvent,
                        repoEventIdMap = repoEventIdMap
                    )
                } catch (e: Throwable) {
                    logger.warn("[$pipelineId]|webhookTriggerPipelineBuild fail: $e", e)
                }
            }
            /* #3131,当对mr的commit check有强依赖，但是蓝盾与git的commit check交互存在一定的时延，可以增加双重锁。
                git发起mr时锁住mr,称为webhook锁，由蓝盾主动发起解锁，解锁有三种情况：
                1. 仓库没有配置蓝盾的流水线，需要解锁
                2. 仓库配置了蓝盾流水线，但是流水线都不需要锁住mr，需要解锁
                3. 仓库配置了蓝盾流水线并且需要锁住mr，需要等commit check发送完成，再解锁
                 @see com.tencent.devops.plugin.service.git.CodeWebhookService.addGitCommitCheck
             */
            gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
            return true
        } finally {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "webhook event repository cache: ${JsonUtil.toJson(EventCacheUtil.getAll(), false)}"
                )
            }
            EventCacheUtil.remove()
        }
    }

    private fun saveTriggerEvent(
        projectId: String,
        builder: PipelineTriggerDetailBuilder,
        triggerEvent: PipelineTriggerEvent,
        repoEventIdMap: MutableMap<String, Long>
    ) {
        if (!builder.getEventSource().isNullOrBlank()) {
            val eventSource = builder.getEventSource()!!
            val eventType = triggerEvent.eventType
            triggerEvent.eventSource = eventSource
            triggerEvent.projectId = projectId
            val eventId = repoEventIdMap[builder.getEventSource()] ?: run {
                val eventId = pipelineTriggerEventService.getEventId(
                    projectId = projectId, requestId = triggerEvent.requestId, eventSource = triggerEvent.eventSource!!
                )
                repoEventIdMap[builder.getEventSource()!!] = eventId
                eventId
            }
            triggerEvent.eventId = eventId
            builder.eventId(eventId)
            builder.detailId(pipelineTriggerEventService.getDetailId())
            val triggerDetail = builder.build()
            pipelineTriggerEventService.saveEvent(
                triggerEvent = triggerEvent,
                triggerDetail = triggerDetail
            )
            // 判断刷新的eventType和repository_hash_id字段的准确性,为后期优化做准备
            pipelineWebhookService.get(
                projectId = projectId,
                pipelineId = triggerDetail.pipelineId!!,
                repositoryHashId = eventSource,
                eventType = eventType
            ) ?: run {
                logger.warn(
                    "Failed to match pipeline webhook|$projectId|${triggerDetail.pipelineId}|$eventSource|$eventType"
                )
            }
        }
    }

    @BkTimed
    private fun webhookTriggerPipelineBuild(
        projectId: String,
        pipelineId: String,
        matcher: ScmWebhookMatcher,
        builder: PipelineTriggerDetailBuilder
    ): Boolean {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: return false

        val model = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)?.model
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return false
        }
        // 触发事件保存流水线名称
        builder.pipelineName(pipelineInfo.pipelineName)
        // 获取授权人
        val userId = pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId)
            ?: pipelineInfo.lastModifyUser
        val container = model.getTriggerContainer()
        // 解析变量
        val variables = getDefaultParam(container.params)
        // 补充yaml流水线代码库信息
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            variables[PIPELINE_PAC_REPO_HASH_ID] = it.repoHashId
        }

        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        // 寻找代码触发原子
        container.elements.forEach elements@{ element ->
            if (!element.elementEnabled() || element !is WebHookTriggerElement) {
                logger.info("Trigger element is disable, can not start pipeline")
                return@elements
            }
            val webHookParams = WebhookElementParamsRegistrar.getService(element)
                .getWebhookElementParams(element, PipelineVarUtil.fillVariableMap(variables)) ?: return@elements
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.getRepositoryId().isBlank()) {
                logger.info("repositoryHashId is blank for code trigger pipeline $pipelineId ")
                return@elements
            }

            logger.info("$pipelineId|${element.name}|Get the code trigger pipeline")
            // #2958 如果仓库找不到,会抛出404异常,就不会继续往下遍历
            val repo = try {
                client.get(ServiceRepositoryResource::class).get(
                    projectId,
                    repositoryConfig.getURLEncodeRepositoryId(),
                    repositoryConfig.repositoryType
                ).data
            } catch (e: Exception) {
                null
            }
            if (repo == null) {
                logger.warn("$pipelineId|repo[$repositoryConfig] does not exist")
                return@elements
            }

            val matchResult = matcher.isMatch(projectId, pipelineId, repo, webHookParams)
            if (matchResult.isMatch) {
                try {
                    checkPermission(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId
                    )
                    val webhookCommit = WebhookCommit(
                        userId = userId,
                        pipelineId = pipelineId,
                        version = null,
                        params = WebhookStartParamsRegistrar.getService(element).getStartParams(
                            projectId = projectId,
                            element = element,
                            repo = repo,
                            matcher = matcher,
                            variables = variables,
                            params = webHookParams,
                            matchResult = matchResult
                        ),
                        repositoryConfig = repositoryConfig,
                        repoName = matcher.getRepoName(),
                        commitId = matcher.getRevision(),
                        block = webHookParams.block,
                        eventType = matcher.getEventType(),
                        codeType = matcher.getCodeType()
                    )
                    val buildId =
                        client.getGateway(ServiceScmWebhookResource::class).webhookCommit(projectId, webhookCommit).data
                    logger.info("$pipelineId|$buildId|webhook trigger|(${element.name}|repo(${matcher.getRepoName()})")
                    if (!buildId.isNullOrEmpty()) {
                        pipelineBuildCommitService.create(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            matcher = matcher,
                            repo = repo
                        )
                        val buildDetail = client.getGateway(ServiceBuildResource::class).getBuildDetail(
                            userId = userId,
                            buildId = buildId,
                            pipelineId = pipelineId,
                            projectId = projectId,
                            channelCode = ChannelCode.BS
                        ).data
                        builder.buildId(buildId)
                            .status(PipelineTriggerStatus.SUCCEED.name)
                            .eventSource(eventSource = repo.repoHashId!!)
                            .reason(PipelineTriggerReason.TRIGGER_SUCCESS.name)
                            .buildNum(buildDetail?.buildNum.toString())
                    }
                } catch (permissionException: PermissionForbiddenException) {
                    logger.warn("check permission failed", permissionException)
                    builder.eventSource(repo.repoHashId!!)
                        .status(PipelineTriggerStatus.FAILED.name)
                        .reason(PipelineTriggerReason.TRIGGER_FAILED.name)
                        .reasonDetail(
                            PipelineTriggerFailedErrorCode(
                                errorCode = ProcessMessageCode.BK_AUTHOR_NOT_PIPELINE_EXECUTE_PERMISSION,
                                params = listOf(userId)
                            )
                        )
                    // 当前流水线没有权限触发
                    return false
                } catch (ignore: Exception) {
                    logger.warn("$pipelineId|webhook trigger|(${element.name})|repo(${matcher.getRepoName()})", ignore)
                    builder.eventSource(eventSource = repo.repoHashId!!)
                    builder.status(PipelineTriggerStatus.FAILED.name)
                        .reason(PipelineTriggerReason.TRIGGER_FAILED.name)
                        .reasonDetail(PipelineTriggerFailedMsg(ignore.message ?: ""))
                }
                return true
            } else {
                logger.info(
                    "$pipelineId|webhook trigger match unsuccess|(${element.name})|repo(${matcher.getRepoName()})"
                )
                if (!matchResult.reason.isNullOrBlank()) {
                    builder.eventSource(eventSource = repo.repoHashId!!)
                    failedMatchElements.add(
                        PipelineTriggerFailedMatchElement(
                            elementId = element.id,
                            elementName = element.name,
                            elementAtomCode = element.getAtomCode(),
                            reasonMsg = matchResult.reason!!
                        )
                    )
                }
            }
        }

        // 历史原因,webhook表没有记录eventType,所以查找出来的订阅者可能因为事件类型不匹配,事件不需要记录
        if (!builder.getEventSource().isNullOrBlank()) {
            builder.status(PipelineTriggerStatus.FAILED.name)
                .reason(PipelineTriggerReason.TRIGGER_NOT_MATCH.name)
                .reasonDetail(PipelineTriggerFailedMatch(failedMatchElements))
        }
        return false
    }

    /**
     * 精确匹配webhook触发
     *
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param version 流水线版本
     * @param taskIds 触发器插件,同一代码库同一事件可能配置多个触发器插件
     * @param repoHashId 触发仓库
     * @param matcher 匹配器
     * @param eventId 事件ID
     */
    fun exactMatchPipelineWebhookBuild(
        projectId: String,
        pipelineId: String,
        version: Int?,
        taskIds: List<String>,
        repoHashId: String,
        matcher: ScmWebhookMatcher,
        eventId: Long
    ): WebhookBuildResult {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId = projectId, pipelineId = pipelineId)
            ?: return WebhookBuildResult(
                result = false,
                reasonDetail = PipelineTriggerFailedMsg("pipeline is not found")
            )

        val model = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId, pipelineId = pipelineId, version = version
        )?.model
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return WebhookBuildResult(
                result = false,
                reasonDetail = PipelineTriggerFailedMsg("pipeline model is not found")
            )
        }
        val repository = try {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = repoHashId,
                repositoryType = RepositoryType.ID
            ).data
        } catch (e: Exception) {
            null
        }
        if (repository == null) {
            logger.warn("repository does not exist|$projectId|$repoHashId")
            return WebhookBuildResult(
                result = false,
                reasonDetail = PipelineTriggerFailedMsg("repository is not found")
            )
        }
        val userId = pipelineInfo.lastModifyUser
        val container = model.getTriggerContainer()
        // 解析变量
        val variables = getDefaultParam(container.params)
        // 补充yaml流水线代码库信息
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            variables[PIPELINE_PAC_REPO_HASH_ID] = it.repoHashId
        }
        val triggerElementMap =
            container.elements.filterIsInstance<WebHookTriggerElement>()
                .filter { it.elementEnabled() }
                .associateBy { it.id }
        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        taskIds.forEach { taskId ->
            val triggerElement = triggerElementMap[taskId] ?: return@forEach
            val webHookParams = WebhookElementParamsRegistrar.getService(triggerElement)
                .getWebhookElementParams(triggerElement, PipelineVarUtil.fillVariableMap(variables)) ?: return@forEach
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.repositoryHashId.isNullOrBlank() && repositoryConfig.repositoryName.isNullOrBlank()) {
                logger.info("repositoryHashId is blank for code trigger pipeline $pipelineId ")
                return@forEach
            }
            val matchResult = matcher.isMatch(projectId, pipelineId, repository, webHookParams)
            if (matchResult.isMatch) {
                try {
                    val params = WebhookStartParamsRegistrar.getService(triggerElement).getStartParams(
                        projectId = projectId,
                        element = triggerElement,
                        repo = repository,
                        matcher = matcher,
                        variables = variables,
                        params = webHookParams,
                        matchResult = matchResult
                    )
                    val webhookCommit = WebhookCommit(
                        userId = userId,
                        pipelineId = pipelineId,
                        version = version,
                        params = params,
                        repositoryConfig = repositoryConfig,
                        repoName = matcher.getRepoName(),
                        commitId = matcher.getRevision(),
                        block = webHookParams.block,
                        eventType = matcher.getEventType(),
                        codeType = matcher.getCodeType()
                    )
                    val buildId = client.getGateway(ServiceScmWebhookResource::class)
                        .webhookCommitNew(projectId, webhookCommit).data
                    logger.info(
                        "$pipelineId|${buildId?.id}|webhook trigger|(${triggerElement.name}|" +
                            "repo(${matcher.getRepoName()})"
                    )
                    return WebhookBuildResult(result = true, pipelineInfo = pipelineInfo, buildId = buildId)
                } catch (ignore: Exception) {
                    logger.warn(
                        "$pipelineId|webhook trigger|(${triggerElement.name})|repo(${matcher.getRepoName()})",
                        ignore
                    )
                    return WebhookBuildResult(
                        result = false,
                        pipelineInfo = pipelineInfo,
                        reasonDetail = PipelineTriggerFailedMsg(ignore.message ?: "trigger failed")
                    )
                }
            } else {
                logger.info("webhook trigger match unSuccess|$projectId|$pipelineId|$taskId)")
                failedMatchElements.add(
                    PipelineTriggerFailedMatchElement(
                        elementId = triggerElement.id,
                        elementName = triggerElement.name,
                        elementAtomCode = triggerElement.getAtomCode(),
                        reasonMsg = matchResult.reason ?: "match failed"
                    )
                )
            }
        }
        return WebhookBuildResult(
            result = false,
            pipelineInfo = pipelineInfo,
            reasonDetail = PipelineTriggerFailedMatch(elements = failedMatchElements)
        )
    }

    /**
     * webhookCommitTriggerPipelineBuild 方法是webhook事件触发最后执行方法
     * @link webhookTriggerPipelineBuild 方法接收webhook事件后通过调用网关接口进行分发，从而区分正式和灰度服务
     * @param projectId 项目ID
     * @param webhookCommit webhook事件信息
     *
     */
    fun webhookCommitTriggerPipelineBuild(projectId: String, webhookCommit: WebhookCommit): BuildId? {
        val userId = webhookCommit.userId
        val pipelineId = webhookCommit.pipelineId
        val startParams = webhookCommit.params

        val repoName = webhookCommit.repoName
        // 如果是分支版本的触发，此处一定要传指定的version号，按指定版本触发
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
        if (pipelineInfo.locked == true) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
        }
        // 代码库触发支持仅有分支版本的情况，如果仅有草稿不需要在这里拦截
//        if (pipelineInfo.latestVersionStatus == VersionStatus.COMMITTING) throw ErrorCodeException(
//            errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
//        )
        val version = webhookCommit.version ?: pipelineInfo.version

        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        if (resource == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return null
        }

        // 兼容从旧v1版本下发过来的请求携带旧的变量命名
        val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            paramProperties = resource.model.getTriggerContainer().params,
            paramValues = startParams.mapValues { it.value.toString() }
        )
        val pipelineParamMap = mutableMapOf<String, BuildParameters>()
        pipelineParamMap.putAll(paramMap)
        startParams.forEach {
            if (paramMap.containsKey(it.key)) {
                return@forEach
            }
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                pipelineParamMap[it.key] = BuildParameters(key = it.key, value = it.value)
            } else if (!pipelineParamMap.contains(newVarName)) { // 新变量还不存在，加入
                pipelineParamMap[newVarName] = BuildParameters(key = newVarName, value = it.value)
            }
        }

        val startEpoch = System.currentTimeMillis()
        try {
            val buildId = pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipelineInfo,
                startType = StartType.WEB_HOOK,
                pipelineParamMap = HashMap(pipelineParamMap),
                channelCode = pipelineInfo.channelCode,
                isMobile = false,
                resource = resource,
                signPipelineVersion = version,
                frequencyLimit = false
            )
            pipelineWebHookQueueService.onWebHookTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId.id,
                variables = webhookCommit.params
            )
            // #2958 webhook触发在触发原子上输出变量
            buildLogPrinter.addLines(
                buildId = buildId.id,
                logMessages = pipelineParamMap.map {
                    LogMessage(
                        message = "${it.key}=${it.value.value}",
                        timestamp = System.currentTimeMillis(),
                        tag = startParams[PIPELINE_START_TASK_ID]?.toString() ?: ""
                    )
                }
            )
            if (buildId.id.isNotBlank()) {
                webhookBuildParameterService.save(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId.id,
                    buildParameters = pipelineParamMap.values.toList()
                )

                // 上报项目用户度量
                if (startParams[PIPELINE_START_WEBHOOK_USER_ID] != null) {
                    uploadProjectUserMetrics(
                        userId = startParams[PIPELINE_START_WEBHOOK_USER_ID]!!.toString(),
                        projectId = projectId,
                        theDate = LocalDate.now()
                    )
                }
            }
            return buildId
        } catch (ignore: Exception) {
            logger.warn("[$pipelineId]| webhook trigger fail to start repo($repoName): ${ignore.message}", ignore)
            throw ignore
        } finally {
            logger.info("$pipelineId|WEBHOOK_TRIGGER|repo=$repoName|time=${System.currentTimeMillis() - startEpoch}")
        }
    }

    private fun checkPermission(userId: String, projectId: String, pipelineId: String) {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EXECUTE,
            message = I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT,
                params = arrayOf(userId, projectId, AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
    }

    private fun uploadProjectUserMetrics(
        userId: String,
        projectId: String,
        theDate: LocalDate
    ) {
        try {
            val projectUserOperateMetricsKey = ProjectUserOperateMetricsData(
                projectId = projectId,
                userId = userId,
                operate = WEBHOOK_COMMIT_TRIGGER,
                theDate = theDate
            ).getProjectUserOperateMetricsKey()
            measureEventDispatcher.dispatch(
                ProjectUserDailyEvent(
                    projectId = projectId,
                    userId = userId,
                    theDate = theDate
                ),
                ProjectUserOperateMetricsEvent(
                    userOperateCounterData = UserOperateCounterData().apply {
                        this.increment(projectUserOperateMetricsKey)
                    }
                )
            )
        } catch (ignored: Exception) {
            logger.error("save auth user metrics", ignored)
        }
    }

    private fun getDefaultParam(params: List<BuildFormProperty>): MutableMap<String, String> {
        val variables = mutableMapOf<String, String>()
        params.forEach { param ->
            variables[param.id] = if (CascadePropertyUtils.supportCascadeParam(param.type) &&
                param.defaultValue is Map<*, *>
            ) {
                JsonUtil.toJson(param.defaultValue, false)
            } else {
                param.defaultValue.toString()
            }
        }
        return variables
    }
}
