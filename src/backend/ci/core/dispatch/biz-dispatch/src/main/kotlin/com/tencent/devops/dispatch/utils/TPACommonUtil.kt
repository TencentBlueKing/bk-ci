package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.QueueFailureException
import com.tencent.devops.dispatch.pojo.QueueRetryException
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.process.engine.common.VMUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TPACommonUtil @Autowired constructor(
    private val commonConfig: CommonConfig,
    private val buildLogPrinter: BuildLogPrinter
) {
    fun logI18n(
        dispatchData: ThirdPartyAgentDispatchData,
        messageCode: String,
        param: Array<String>? = null,
        suffixMsg: String = ""
    ) {
        log(
            dispatchData,
            I18nUtil.getCodeLanMessage(
                messageCode = messageCode,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = param
            ) + suffixMsg
        )
    }

    fun log(dispatchData: ThirdPartyAgentDispatchData, logMessage: String) {
        buildLogPrinter.addLine(
            buildId = dispatchData.buildId,
            message = logMessage,
            tag = VMUtils.genStartVMTaskId(dispatchData.vmSeqId),
            containerHashId = dispatchData.containerHashId,
            executeCount = dispatchData.executeCount ?: 1,
            jobId = dispatchData.jobId,
            stepId = VMUtils.genStartVMTaskId(dispatchData.vmSeqId)
        )
    }

    fun logWarnI18n(
        dispatchData: ThirdPartyAgentDispatchData,
        messageCode: String,
        param: Array<String>? = null,
        suffixMsg: String = ""
    ) {
        logWarn(
            dispatchData,
            I18nUtil.getCodeLanMessage(
                messageCode = messageCode,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = param
            ) + suffixMsg
        )
    }

    fun logWarn(dispatchData: ThirdPartyAgentDispatchData, logMessage: String) {
        buildLogPrinter.addYellowLine(
            buildId = dispatchData.buildId,
            message = logMessage,
            tag = VMUtils.genStartVMTaskId(dispatchData.vmSeqId),
            containerHashId = dispatchData.containerHashId,
            executeCount = dispatchData.executeCount ?: 1,
            jobId = dispatchData.jobId,
            stepId = null
        )
    }

    fun logDebugI18n(
        dispatchData: ThirdPartyAgentDispatchData,
        messageCode: String,
        param: Array<String>? = null,
        preMsg: String = "",
        suffixMsg: String = ""
    ) {
        logDebug(
            dispatchData,
            preMsg + I18nUtil.getCodeLanMessage(
                messageCode = messageCode,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = param
            ) + suffixMsg
        )
    }

    fun logDebug(dispatchData: ThirdPartyAgentDispatchData, message: String) {
        buildLogPrinter.addDebugLine(
            buildId = dispatchData.buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(dispatchData.vmSeqId),
            containerHashId = dispatchData.containerHashId,
            executeCount = dispatchData.executeCount ?: 1,
            jobId = dispatchData.jobId,
            stepId = VMUtils.genStartVMTaskId(dispatchData.vmSeqId)
        )
    }

    fun logWithBuildUrl(
        data: ThirdPartyAgentDispatchData,
        messageCode: String,
        param: Array<String>? = null,
        pipelineId: String,
        lockedBuildId: String,
        linkTip: String
    ) {
        val host = HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
        val link = "$host/console/pipeline/${data.projectId}/$pipelineId/detail/$lockedBuildId"
        val msg = if (lockedBuildId != data.buildId) {
            "$linkTip<a target='_blank' href='$link'>$lockedBuildId</a>"
        } else {
            linkTip
        }

        logI18n(data, messageCode, param, suffixMsg = msg)
    }

    fun logWithAgentUrl(
        data: ThirdPartyAgentDispatchData,
        messageCode: String,
        param: Array<String>? = null,
        nodeHashId: String?
    ) {
        val host = HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
        // 跨项目使用 agent
        val projectId = if (data.dispatchType is ThirdPartyAgentEnvDispatchType) {
            data.dispatchType.envProjectId ?: data.projectId
        } else {
            data.projectId
        }
        val link = "$host/console/environment/$projectId/nodeDetail/$nodeHashId"
        val msg = if (nodeHashId.isNullOrBlank()) {
            ""
        } else {
            "<a target='_blank' href='$link'>$nodeHashId</a>"
        }

        logI18n(data, messageCode, param, suffixMsg = msg)
    }

    fun logError(
        data: ThirdPartyAgentDispatchData,
        message: String
    ) {
        buildLogPrinter.addRedLine(
            buildId = data.buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(data.vmSeqId),
            containerHashId = data.containerHashId,
            executeCount = data.executeCount ?: 1,
            jobId = data.jobId,
            stepId = VMUtils.genStartVMTaskId(data.vmSeqId)
        )
    }

    companion object {
        fun queueRetry(
            errorCode: ErrorCodeEnum,
            errMsg: String? = null,
            suffixMsg: String = ""
        ): QueueRetryException {
            throw QueueRetryException(
                errorCodeEnum = errorCode,
                errorMessage = (errMsg ?: errorCode.getErrorMessage()) + suffixMsg
            )
        }

        fun queueFailureI18n(
            errorCode: ErrorCodeEnum,
            messageCode: String? = null,
            param: Array<String>? = null
        ): QueueFailureException {
            return queueFailure(
                errorCode,
                I18nUtil.getCodeLanMessage(
                    messageCode = messageCode ?: errorCode.errorCode.toString(),
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = param
                )
            )
        }

        fun queueFailure(
            errorCode: ErrorCodeEnum,
            errMsg: String
        ): QueueFailureException {
            return QueueFailureException(
                errorType = errorCode.errorType,
                errorCode = errorCode.errorCode,
                formatErrorMessage = errorCode.formatErrorMessage,
                errorMessage = errMsg
            )
        }
    }
}