package com.tencent.devops.gitci.trigger.exception

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.common.exception.CommitCheck
import com.tencent.devops.gitci.common.exception.ErrorCodeEnum
import com.tencent.devops.gitci.common.exception.TriggerBaseException
import com.tencent.devops.gitci.common.exception.TriggerException
import com.tencent.devops.gitci.common.exception.TriggerThirdException
import com.tencent.devops.gitci.common.exception.Yamls
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.GitCICommitCheckState
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting
import com.tencent.devops.gitci.trigger.GitCIEventService
import com.tencent.devops.gitci.trigger.GitCITriggerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerExceptionService @Autowired constructor(
    private val scmClient: ScmClient,
    private val gitCIEventService: GitCIEventService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerExceptionService::class.java)
    }

    var requestEvent: GitRequestEvent? = null
    var gitEvent: GitEvent? = null
    var basicSetting: GitCIBasicSetting? = null

    fun <T> handle(action: () -> T?): T? {
        try {
            return action()
        } catch (triggerE: TriggerBaseException) {
            return handleTriggerException(triggerE)
        } catch (e: Throwable) {
            // 触发只要出了异常就把Mr锁定取消，防止出现工蜂项目无法合并
            logger.error("Trigger handle catch Throwable ${e.message}")
            return if (requestEvent == null || basicSetting == null || gitEvent == null) {
                null
            } else {
                val mrEvent = gitEvent is GitMergeRequestEvent
                if (basicSetting!!.enableMrBlock && mrEvent) {
                    noPipelineCommitCheck(
                        gitRequestEvent = requestEvent!!,
                        block = false,
                        state = GitCICommitCheckState.FAILURE,
                        gitCIBasicSetting = basicSetting!!
                    )
                }
                gitCIEventService.saveTriggerNotBuildEvent(
                    userId = requestEvent!!.userId,
                    eventId = requestEvent!!.id!!,
                    reason = TriggerReason.UNKNOWN_ERROR.name,
                    reasonDetail = TriggerReason.UNKNOWN_ERROR.detail.format(e.message),
                    gitProjectId = requestEvent!!.gitProjectId
                )
                null
            }
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
                gitProjectId = gitRequestEvent.gitProjectId
            )
            if (triggerE.commitCheck != null) {
                // 没有yaml前只有无流水线commitCheck
                noPipelineCommitCheck(
                    gitRequestEvent = triggerE.requestEvent,
                    block = triggerE.commitCheck.block,
                    state = triggerE.commitCheck.state,
                    gitCIBasicSetting = triggerE.basicSetting!!
                )
            }
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
                    version = version
                )
            }
            return null
        }
    }

    // 针对错误码(多为GitApi)请求做异常处理
    fun <T> handleErrorCode(
        request: GitRequestEvent,
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
                    Pair(error.name, if (triggerE.errorMessage.isNullOrBlank()) {
                        error.formatErrorMessage
                    } else {
                        triggerE.errorMessage.format(triggerE.messageParams)
                    })
                }
            }
            else -> Pair("", "")
        }
    }

    private fun noPipelineCommitCheck(
        gitRequestEvent: GitRequestEvent,
        gitCIBasicSetting: GitCIBasicSetting,
        block: Boolean,
        state: GitCICommitCheckState
    ) {
        scmClient.pushCommitCheckWithBlock(
            commitId = gitRequestEvent.commitId,
            mergeRequestId = gitRequestEvent.mergeRequestId ?: 0L,
            userId = gitRequestEvent.userId,
            block = block,
            state = state,
            context = GitCITriggerService.noPipelineBuildEvent,
            gitCIBasicSetting = gitCIBasicSetting,
            jumpRequest = false,
            description = null
        )
    }
}
