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

package com.tencent.devops.process.trigger

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.MANUAL_START_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.OPENAPI_START_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.PIPELINE_START_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.REMOTE_START_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TIMING_START_EVENT_DESC
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.code.WebhookBuildResult
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatch
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_START_REMOTE_CLIENT_IP
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Aspect
class PipelineTriggerEventAspect(
    private val triggerEventService: PipelineTriggerEventService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerEventAspect::class.java)
    }

    @Around("execution(* com.tencent.devops.process.service.pipeline.PipelineBuildService.startPipeline(..))")
    fun aroundStartPipeline(pjp: ProceedingJoinPoint): Any? {
        var result: Any? = null
        var exception: Throwable? = null
        try {
            result = pjp.proceed()
            return result
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            saveTriggerEvent(pjp = pjp, result = result, exception = exception)
        }
    }

    @Around("execution(* com.tencent.devops.process.service.webhook." +
            "PipelineBuildWebhookService.exactMatchPipelineWebhookBuild(..))")
    fun aroundPipelineWebhookBuild(pjp: ProceedingJoinPoint): Any? {
        var result: Any? = null
        var exception: Throwable? = null
        try {
            result = pjp.proceed()
            return result
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            saveWebhookTriggerEvent(pjp = pjp, result = result, exception = exception)
        }
    }

    private fun saveTriggerEvent(pjp: ProceedingJoinPoint, result: Any?, exception: Throwable?) {
        try {
            // openapi/远程触发/定时触发/子流水线触发 如果执行成功,则不记录事件，减少数据量
            if (result != null && exception == null) {
                return
            }
            // 参数value
            val parameterValue = pjp.args
            // 参数key
            val parameterNames = (pjp.signature as MethodSignature).parameterNames
            var pipeline: PipelineInfo? = null
            var userId: String? = null
            var channelCode: ChannelCode? = null
            var startValues: Map<String, String>? = null
            var pipelineParamMap: MutableMap<String, BuildParameters>? = null
            var startType: StartType? = null

            for (index in parameterValue.indices) {
                when (parameterNames[index]) {
                    "pipeline" -> pipeline = parameterValue[index] as PipelineInfo
                    "userId" -> userId = parameterValue[index]?.toString()
                    "channelCode" -> channelCode = parameterValue[index] as ChannelCode
                    "startValues" -> startValues = parameterValue[index] as Map<String, String>?
                    "startType" -> startType = parameterValue[index] as StartType
                    "pipelineParamMap" ->
                        pipelineParamMap = parameterValue[index] as MutableMap<String, BuildParameters>?
                    else -> Unit
                }
            }
            // 判断是否应该跳过保存触发事件
            val isSkip = skipSaveTriggerEvent(
                pipeline = pipeline,
                userId = userId,
                channelCode = channelCode,
                startType = startType,
                pipelineParamMap = pipelineParamMap
            )
            if (isSkip) {
                return
            }
            val triggerEvent =
                buildTriggerEvent(
                    userId = userId!!,
                    projectId = pipeline!!.projectId,
                    startValues = startValues,
                    pipelineParamMap = pipelineParamMap,
                    startType = startType!!
                ) ?: return

            val triggerDetail = buildTriggerDetail(
                pipeline = pipeline,
                eventId = triggerEvent.eventId!!,
                result = result,
                exception = exception,
                reasonDetail = null
            )
            triggerEventService.saveEvent(
                triggerEvent = triggerEvent,
                triggerDetail = triggerDetail
            )
        } catch (ignored: Throwable) {
            // 为了不影响业务,保存事件异常不抛出
            logger.warn("Failed to save trigger event", ignored)
        }
    }

    private fun saveWebhookTriggerEvent(pjp: ProceedingJoinPoint, result: Any?, exception: Throwable?) {
        try {
            // 参数value
            val parameterValue = pjp.args
            // 参数key
            val parameterNames = (pjp.signature as MethodSignature).parameterNames
            var pipelineInfo: PipelineInfo? = null
            var buildId: BuildId? = null
            var eventId: Long? = null
            var reasonDetail: PipelineTriggerReasonDetail? = null
            for (index in parameterValue.indices) {
                when (parameterNames[index]) {
                    "eventId" -> eventId = parameterValue[index] as Long
                    else -> Unit
                }
            }
            if (result != null) {
                val webhookBuildResult = result as WebhookBuildResult
                buildId = webhookBuildResult.buildId
                pipelineInfo = webhookBuildResult.pipelineInfo
                reasonDetail = webhookBuildResult.reasonDetail
            }
            if (pipelineInfo == null || eventId == null) {
                return
            }

            val triggerDetail = buildTriggerDetail(
                pipeline = pipelineInfo,
                eventId = eventId,
                result = buildId,
                exception = exception,
                reasonDetail = reasonDetail
            )
            triggerEventService.saveTriggerDetail(triggerDetail)
        } catch (ignored: Throwable) {
            // 为了不影响业务,保存事件异常不抛出
            logger.warn("Failed to save webhook trigger event", ignored)
        }
    }

    @Suppress("ComplexCondition")
    private fun skipSaveTriggerEvent(
        pipeline: PipelineInfo?,
        userId: String?,
        channelCode: ChannelCode?,
        startType: StartType?,
        pipelineParamMap: MutableMap<String, BuildParameters>?
    ): Boolean {
        if (pipeline == null || userId == null || channelCode == null || startType == null) {
            return true
        }
        // 不是BS渠道的不需要记录,webhook的在webhook触发时已记录,在这不需要记录
        if (channelCode != ChannelCode.BS || startType == StartType.WEB_HOOK) {
            return true
        }
        // 重试不需要记录
        return pipelineParamMap != null && pipelineParamMap[PIPELINE_RETRY_BUILD_ID] != null
    }

    @Suppress("ComplexCondition")
    private fun buildTriggerEvent(
        userId: String,
        projectId: String,
        startValues: Map<String, String>?,
        pipelineParamMap: MutableMap<String, BuildParameters>?,
        startType: StartType
    ): PipelineTriggerEvent? {
        val requestId = MDC.get(TraceTag.BIZID)
        val eventId = triggerEventService.getEventId()
        val triggerEventBuilder = PipelineTriggerEventBuilder()
        triggerEventBuilder.requestId(requestId)
        triggerEventBuilder.projectId(projectId)
        triggerEventBuilder.eventId(eventId)
        triggerEventBuilder.triggerUser(userId)
        triggerEventBuilder.createTime(LocalDateTime.now())
        startValues?.let { triggerEventBuilder.requestParams(it) }

        when (startType) {
            StartType.MANUAL -> {
                triggerEventBuilder.triggerType(PipelineTriggerType.MANUAL.name)
                triggerEventBuilder.eventSource(userId)
                triggerEventBuilder.eventType(PipelineTriggerType.MANUAL.name)
                triggerEventBuilder.eventDesc(
                    I18Variable(
                        code = MANUAL_START_EVENT_DESC,
                        params = listOf(userId)
                    ).toJsonStr()
                )
            }

            StartType.REMOTE -> {
                val eventSource = startValues?.get(PIPELINE_START_REMOTE_CLIENT_IP) ?: userId
                triggerEventBuilder.triggerType(PipelineTriggerType.REMOTE.name)
                triggerEventBuilder.eventSource(eventSource)
                triggerEventBuilder.eventType(PipelineTriggerType.REMOTE.name)
                triggerEventBuilder.eventDesc(
                    I18Variable(
                        code = REMOTE_START_EVENT_DESC,
                        params = listOf(eventSource, userId)
                    ).toJsonStr()
                )
            }

            StartType.TIME_TRIGGER -> {
                triggerEventBuilder.triggerType(PipelineTriggerType.TIME_TRIGGER.name)
                triggerEventBuilder.eventSource(userId)
                triggerEventBuilder.eventType(PipelineTriggerType.TIME_TRIGGER.name)
                triggerEventBuilder.eventDesc(
                    I18Variable(
                        code = TIMING_START_EVENT_DESC,
                        params = listOf(userId)
                    ).toJsonStr()
                )
            }

            StartType.PIPELINE -> {
                val parentProjectId = pipelineParamMap?.get(PIPELINE_START_PARENT_PROJECT_ID)?.value?.toString()
                val parentPipelineId = pipelineParamMap?.get(PIPELINE_START_PARENT_PIPELINE_ID)?.value?.toString()
                val parentPipelineName = pipelineParamMap?.get(PIPELINE_START_PARENT_PIPELINE_NAME)?.value?.toString()
                val parentBuildId = pipelineParamMap?.get(PIPELINE_START_PARENT_BUILD_ID)?.value?.toString()
                if (parentProjectId == null || parentPipelineId == null ||
                    parentBuildId == null || parentPipelineName == null
                ) {
                    return null
                }
                triggerEventBuilder.triggerType(PipelineTriggerType.PIPELINE.name)
                triggerEventBuilder.eventSource(parentPipelineId)
                triggerEventBuilder.eventType(PipelineTriggerType.PIPELINE.name)
                triggerEventBuilder.eventDesc(
                    I18Variable(
                        code = PIPELINE_START_EVENT_DESC,
                        params = listOf(
                            userId,
                            "/console/pipeline/$parentProjectId/$parentPipelineId/detail/$parentBuildId",
                            parentPipelineName
                        )
                    ).toJsonStr()
                )
            }
            // 目前channel为BS的,只有openapi的startType为service
            StartType.SERVICE -> {
                triggerEventBuilder.triggerType(PipelineTriggerType.OPENAPI.name)
                triggerEventBuilder.eventSource(userId)
                triggerEventBuilder.eventType(PipelineTriggerType.OPENAPI.name)
                triggerEventBuilder.eventDesc(
                    I18Variable(
                        code = OPENAPI_START_EVENT_DESC,
                        params = listOf(userId)
                    ).toJsonStr()
                )
            }

            else -> return null
        }
        return triggerEventBuilder.build()
    }

    private fun buildTriggerDetail(
        pipeline: PipelineInfo,
        eventId: Long,
        result: Any?,
        exception: Throwable?,
        reasonDetail: PipelineTriggerReasonDetail?
    ): PipelineTriggerDetail {
        val triggerDetailBuilder = PipelineTriggerDetailBuilder()
        triggerDetailBuilder.eventId(eventId)
        triggerDetailBuilder.projectId(pipeline.projectId)
        triggerDetailBuilder.pipelineId(pipelineId = pipeline.pipelineId)
        triggerDetailBuilder.pipelineName(pipeline.pipelineName)
        triggerDetailBuilder.detailId(triggerEventService.getDetailId())

        when {
            result != null -> {
                val buildId = result as BuildId
                triggerDetailBuilder.status(PipelineTriggerStatus.SUCCEED.name)
                triggerDetailBuilder.reason(PipelineTriggerReason.TRIGGER_SUCCESS.name)
                triggerDetailBuilder.buildId(buildId.id)
                triggerDetailBuilder.buildNum(buildId.num?.toString() ?: "")
            }

            reasonDetail != null -> {
                triggerDetailBuilder.status(PipelineTriggerStatus.FAILED.name)
                if (reasonDetail is PipelineTriggerFailedMatch) {
                    triggerDetailBuilder.reason(PipelineTriggerReason.TRIGGER_NOT_MATCH.name)
                } else {
                    triggerDetailBuilder.reason(PipelineTriggerReason.TRIGGER_FAILED.name)
                }

                triggerDetailBuilder.reasonDetail(reasonDetail)
            }

            exception != null -> {
                val exceptionReasonDetail = when (exception) {
                    is ErrorCodeException -> PipelineTriggerFailedErrorCode(
                        errorCode = exception.errorCode,
                        params = exception.params?.toList()
                    )

                    else -> PipelineTriggerFailedMsg(exception.message ?: "unknown error")
                }
                triggerDetailBuilder.status(PipelineTriggerStatus.FAILED.name)
                triggerDetailBuilder.reason(PipelineTriggerReason.TRIGGER_FAILED.name)
                triggerDetailBuilder.reasonDetail(exceptionReasonDetail)
            }
        }
        return triggerDetailBuilder.build()
    }
}
