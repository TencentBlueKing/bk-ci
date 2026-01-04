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

package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_PAC_REPO_HASH_ID
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.process.api.service.ServiceWebhookBuildResource
import com.tencent.devops.process.constant.MeasureConstant
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.ScmWebhookEventBody
import com.tencent.devops.process.pojo.webhook.WebhookStartPipelineRequest
import com.tencent.devops.process.service.pipeline.PipelineYamlVersionResolver
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.PipelineTriggerMeasureService
import com.tencent.devops.process.trigger.event.ScmWebhookTriggerEvent
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import io.micrometer.core.instrument.Tags
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebhookTriggerBuildService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val webhookTriggerMatcher: WebhookTriggerMatcher,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelineYamlVersionResolver: PipelineYamlVersionResolver,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val pipelineTriggerMeasureService: PipelineTriggerMeasureService,
    private val client: Client,
    private val simpleRateLimiter: SimpleRateLimiter,
    private val sampleEventDispatcher: SampleEventDispatcher
) {
    @Value("\${scm.webhook.trigger.max.count:$SCM_WEBHOOK_TRIGGER_MAX_COUNT_DEFAULT}")
    private val scmWebhookTriggerMaxCount: Int = SCM_WEBHOOK_TRIGGER_MAX_COUNT_DEFAULT

    fun trigger(event: ScmWebhookTriggerEvent) {
        // 同一个项目,最大处理并发数
        val lockKey = "ScmWebhookRateLimit:${event.projectId}"
        var acquire = false
        try {
            acquire = simpleRateLimiter.acquire(
                scmWebhookTriggerMaxCount, lockKey = lockKey
            )
            if (!acquire) {
                logger.info("scm webhook trigger acquire rate limit|$lockKey")
                event.retry()
                return
            }
            with(event) {
                logger.info(
                    "start to trigger pipeline|$eventId|$projectId|$pipelineId|$version|${repository.repoHashId}"
                )
                val triggerEvent = pipelineTriggerEventService.getTriggerEvent(
                    projectId = projectId, eventId = eventId
                ) ?: run {
                    logger.info("trigger event not found|$eventId")
                    return
                }
                val webhook = triggerEvent.eventBody?.let { (it as ScmWebhookEventBody).webhook } ?: run {
                    logger.info("webhook event body is empty|$eventId")
                    return
                }
                trigger(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version,
                    eventId = eventId,
                    repository = repository,
                    webhook = webhook,
                    eventTime = eventTime
                )
            }
        } finally {
            if (acquire) {
                simpleRateLimiter.release(lockKey = lockKey)
            }
        }
    }

    private fun ScmWebhookTriggerEvent.retry() {
        logger.info("ENGINE|$eventId|$projectId|$pipelineId|RETRY_TO_WEBHOOK_TRIGGER")
        this.delayMills = DEFAULT_DELAY
        sampleEventDispatcher.dispatch(this)
    }

    @Suppress("CyclomaticComplexMethod")
    fun trigger(
        projectId: String,
        pipelineId: String,
        version: Int?,
        eventId: Long,
        repository: Repository,
        webhook: Webhook,
        eventTime: LocalDateTime? = null,
        isYaml: Boolean = false
    ) {
        val context = WebhookTriggerContext(projectId = projectId, pipelineId = pipelineId, eventId = eventId)
        var status = PipelineTriggerReason.TRIGGER_SUCCESS
        val watcher = Watcher("new WebhookTrigger|$projectId|$pipelineId|$version|${repository.repoHashId}")
        try {
            watcher.start("get pipeline info")
            val pipelineInfo =
                pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                    params = arrayOf(pipelineId)
                )
            context.pipelineInfo = pipelineInfo

            watcher.start("get pipeline resource version")
            val resource = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, version)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                )
            val model = resource.model

            val variables = mutableMapOf<String, String>()
            val container = model.stages[0].containers[0] as TriggerContainer
            // 解析变量
            container.params.forEach { param ->
                variables[param.id] = param.defaultValue.toString()
            }
            // 填充[variables.]前缀
            variables.putAll(PipelineVarUtil.fillVariableMap(variables))
            if (repository.enablePac == true) {
                variables[PIPELINE_PAC_REPO_HASH_ID] = repository.repoHashId!!
            }
            val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
            watcher.start("match element")
            container.elements.filterIsInstance<WebHookTriggerElement>().forEach elements@{ element ->
                if (!element.elementEnabled()) {
                    return@elements
                }
                val atomResponse = webhookTriggerMatcher.matches(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repository = repository,
                    webhook = webhook,
                    variables = variables,
                    element = element
                )
                when (atomResponse.matchStatus) {
                    MatchStatus.REPOSITORY_NOT_MATCH,
                    MatchStatus.ELEMENT_NOT_MATCH,
                    MatchStatus.EVENT_TYPE_NOT_MATCH -> return@elements

                    MatchStatus.CONDITION_NOT_MATCH -> {
                        failedMatchElements.add(
                            PipelineTriggerFailedMatchElement(
                                elementId = element.id,
                                elementName = element.name,
                                elementAtomCode = element.getAtomCode(),
                                reasonMsg = atomResponse.failedReason ?: I18Variable(
                                    code = TRIGGER_CONDITION_NOT_MATCH
                                ).toJsonStr()
                            )
                        )
                    }

                    MatchStatus.SUCCESS -> {
                        watcher.start("start pipeline")
                        startPipeline(
                            context = context,
                            pipelineInfo = pipelineInfo,
                            resource = resource,
                            startParams = atomResponse.outputVars
                        )
                        return
                    }
                }
            }
            if (failedMatchElements.isNotEmpty()) {
                watcher.start("match failed")
                status = PipelineTriggerReason.TRIGGER_NOT_MATCH
                context.failedMatchElements = failedMatchElements
                webhookTriggerManager.fireMatchFailed(context)
            }
        } catch (ignored: Exception) {
            status = PipelineTriggerReason.TRIGGER_FAILED
            logger.error(
                "Failed to trigger by webhook|$eventId|$projectId|$pipelineId|${repository.repoHashId}",
                ignored
            )
            webhookTriggerManager.fireError(context, ignored)
        } finally {
            LogUtils.printCostTimeWE(watcher)
            eventTime?.let {
                val timeConsumingMills = System.currentTimeMillis() - it.timestampmilli()
                pipelineTriggerMeasureService.recordTaskExecutionTime(
                    name = MeasureConstant.PIPELINE_SCM_WEBHOOK_EXECUTE_TIME,
                    tags = Tags.of(MeasureConstant.TAG_SCM_WEBHOOK_TRIGGER_STATUS, status.name)
                        .and(MeasureConstant.TAG_SCM_WEBHOOK_TRIGGER_YAML, isYaml.toString())
                        .and(MeasureConstant.TAG_SCM_WEBHOOK_TRIGGER_OLD, "false")
                        .toList(),
                    timeConsumingMills = timeConsumingMills
                )
            }
        }
    }

    fun yamlTrigger(event: PipelineYamlFileEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|Start to trigger yaml pipeline|$eventId|$projectId|$repoHashId|" +
                        "$filePath|$ref|$blobId"
            )
            val triggerEvent = pipelineTriggerEventService.getTriggerEvent(
                projectId = projectId, eventId = eventId
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
                params = arrayOf(eventId.toString())
            )
            val eventBody = triggerEvent.eventBody ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_BODY_NOT_FOUND,
                params = arrayOf(eventId.toString())
            )
            val pipelineYamlVersion = pipelineYamlVersionResolver.getPipelineYamlVersion(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                blobId = blobId!!,
                defaultBranch = defaultBranch
            ) ?: run {
                logger.info(
                    "[PAC_PIPELINE]|trigger yaml pipeline not found pipeline version|$eventId|" +
                        "$projectId|$repoHashId|$filePath|$blobId"
                )
                return
            }
            logger.info(
                "[PAC_PIPELINE]|find yaml pipeline trigger version|$eventId|" +
                    "$projectId|$repoHashId|$filePath|$ref|$blobId|" +
                    "${pipelineYamlVersion.pipelineId}|${pipelineYamlVersion.version}"
            )
            trigger(
                projectId = projectId,
                pipelineId = pipelineYamlVersion.pipelineId,
                version = pipelineYamlVersion.version,
                eventId = eventId,
                repository = repository,
                webhook = (eventBody as ScmWebhookEventBody).webhook,
                eventTime = eventTime,
                isYaml = true
            )
        }
    }

    private fun startPipeline(
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        startParams: Map<String, Any>
    ) {
        val startEpoch = System.currentTimeMillis()
        if (pipelineInfo.locked == true) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
        }
        // 代码库触发支持仅有分支版本的情况，如果仅有草稿需要在这里拦截
        if (pipelineInfo.latestVersionStatus == VersionStatus.COMMITTING) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
        )
        val (projectId, pipelineId) = pipelineInfo.projectId to pipelineInfo.pipelineId
        val userId = pipelineRepositoryService.getPipelineOauthUser(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: pipelineInfo.lastModifyUser
        val buildId = client.getGateway(ServiceWebhookBuildResource::class).webhookStartPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            request = WebhookStartPipelineRequest(
                pipelineInfo = pipelineInfo,
                startType = StartType.WEB_HOOK,
                pipelineParamMap = convertBuildParameters(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    triggerContainer = resource.model.getTriggerContainer(),
                    startParams = startParams
                ),
                channelCode = pipelineInfo.channelCode,
                resource = resource,
                signPipelineVersion = resource.version,
                frequencyLimit = false
            )
        ).data
        logger.info(
            "success to trigger by webhook|${context.eventId}|$projectId|$pipelineId|${resource.version}"
        )
        context.buildId = buildId
        context.startParams = startParams
        webhookTriggerManager.fireBuildSuccess(context = context)
    }

    private fun convertBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerContainer: TriggerContainer,
        startParams: Map<String, Any>
    ): MutableMap<String, BuildParameters> {
        val pipelineParamMap = mutableMapOf<String, BuildParameters>()
        val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            paramProperties = triggerContainer.params,
            paramValues = startParams.mapValues { it.value.toString() }
        )
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
        return pipelineParamMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookTriggerBuildService::class.java)
        private const val SCM_WEBHOOK_TRIGGER_MAX_COUNT_DEFAULT = 100
        private const val DEFAULT_DELAY = 1000
    }
}
