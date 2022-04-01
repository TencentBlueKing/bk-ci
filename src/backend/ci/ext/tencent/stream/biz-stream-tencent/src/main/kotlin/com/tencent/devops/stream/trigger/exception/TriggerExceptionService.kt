package com.tencent.devops.stream.trigger.exception

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.TriggerThirdException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.GitCIEventService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerExceptionService @Autowired constructor(
    private val gitCIEventService: GitCIEventService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerExceptionService::class.java)
    }

    fun <T> handle(
        requestEvent: GitRequestEventForHandle,
        gitEvent: GitEvent?,
        basicSetting: GitCIBasicSetting?,
        action: () -> T?
    ): T? {
        try {
            return action()
        } catch (triggerE: TriggerBaseException) {
            return handleTriggerException(triggerE)
        } catch (e: Throwable) {
            gitCIEventService.saveTriggerNotBuildEvent(
                userId = requestEvent.userId,
                eventId = requestEvent.id!!,
                reason = TriggerReason.UNKNOWN_ERROR.name,
                reasonDetail = TriggerReason.UNKNOWN_ERROR.detail.format(e.message),
                gitProjectId = requestEvent.gitProjectId,
                branch = requestEvent.branch
            )
            return null
        }
    }

    private fun handleTriggerException(triggerE: TriggerBaseException): Nothing? {
        val gitRequestEvent = triggerE.requestEvent
        val (realReason, realReasonDetail) = getReason(triggerE)
        // 为解析yaml之前统一检查
        if (triggerE.pipeline == null && triggerE.filePath == null) {
            gitCIEventService.saveTriggerNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                reason = realReason,
                reasonDetail = realReasonDetail,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch
            )
            return null
        } else {
            with(triggerE) {
                gitCIEventService.saveBuildNotBuildEvent(
                    userId = requestEvent.userId,
                    eventId = requestEvent.id!!,
                    pipelineId = if (pipeline?.pipelineId.isNullOrBlank()) {
                        null
                    } else {
                        pipeline?.pipelineId
                    },
                    pipelineName = pipeline?.displayName,
                    filePath = filePath ?: pipeline!!.filePath,
                    originYaml = yamls?.originYaml,
                    normalizedYaml = yamls?.normalYaml,
                    reason = realReason,
                    reasonDetail = realReasonDetail,
                    gitProjectId = requestEvent.gitProjectId,
                    sendCommitCheck = commitCheck != null,
                    commitCheckBlock = commitCheck?.block ?: false,
                    version = version,
                    branch = requestEvent.branch
                )
            }
            return null
        }
    }

    // 针对错误码(多为GitApi)请求做异常处理
    fun <T> handleErrorCode(
        request: GitRequestEventForHandle,
        messageParams: List<String>? = null,
        event: GitEvent? = null,
        basicSetting: GitCIBasicSetting? = null,
        commitCheck: CommitCheck? = null,
        pipeline: GitProjectPipeline? = null,
        yamls: Yamls? = null,
        version: String? = null,
        filePath: String? = null,
        action: () -> T?
    ): T? {
        return try {
            action()
        } catch (codeE: ErrorCodeException) {
            TriggerThirdException.triggerThirdError(
                request = request,
                event = event,
                commitCheck = commitCheck,
                basicSetting = basicSetting,
                pipeline = pipeline,
                yamls = yamls,
                version = version,
                code = codeE.errorCode,
                message = codeE.defaultMessage,
                messageParams = messageParams,
                filePath = filePath
            )
        }
    }

    fun handleManualTrigger(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            val (errorCode, message) = when (e) {
                is OauthForbiddenException -> {
                    throw e
                }
                is ErrorCodeException -> {
                    Pair(ErrorCodeEnum.MANUAL_TRIGGER_THIRD_PARTY_ERROR, e.defaultMessage)
                }
                is TriggerBaseException -> {
                    val (reason, realReasonDetail) = getReason(e)
                    if (reason == TriggerReason.UNKNOWN_ERROR.name) {
                        Pair(ErrorCodeEnum.MANUAL_TRIGGER_SYSTEM_ERROR, realReasonDetail)
                    } else {
                        Pair(ErrorCodeEnum.MANUAL_TRIGGER_USER_ERROR, realReasonDetail)
                    }
                }
                else -> {
                    Pair(ErrorCodeEnum.MANUAL_TRIGGER_SYSTEM_ERROR, e.message)
                }
            }
            throw ErrorCodeException(
                errorCode = errorCode.errorCode.toString(),
                defaultMessage = message
            )
        }
    }

    private fun getReason(triggerE: TriggerBaseException): Pair<String, String> {
        return when (triggerE) {
            is TriggerException -> {
                Pair(triggerE.triggerReason.name, triggerE.triggerReason.detail.format(triggerE.reasonParams))
            }
            is TriggerThirdException -> {
                val error = try {
                    val code = triggerE.errorCode.toInt()
                    ErrorCodeEnum.get(code)
                } catch (e: NumberFormatException) {
                    null
                }
                if (error == null) {
                    // todo: 未知的不是stream的错误码
                    Pair("", triggerE.errorMessage ?: "")
                } else {
                    Pair(
                        error.name, if (triggerE.errorMessage.isNullOrBlank()) {
                            error.formatErrorMessage
                        } else {
                            triggerE.errorMessage.format(triggerE.messageParams)
                        }
                    )
                }
            }
            else -> Pair("", "")
        }
    }
}
