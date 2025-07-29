package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TPACommonUtil @Autowired constructor(
    private val client: Client,
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
            "$linkTip <a target='_blank' href='$link'>$lockedBuildId</a>"
        } else {
            linkTip
        }

        logI18n(data, messageCode, param, suffixMsg = msg)
    }

    fun logWithAgentUrl(
        data: ThirdPartyAgentDispatchData,
        messageCode: String,
        param: Array<String>? = null,
        nodeHashId: String?,
        agentHashId: String?
    ) {
        val host = HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
        // 跨项目使用 agent
        val projectId = if (data.dispatchType is ThirdPartyAgentEnvDispatchType) {
            data.dispatchType.envProjectId?.ifBlank { data.projectId } ?: data.projectId
        } else {
            data.projectId
        }
        val link = "$host/console/environment/$projectId/node/nodeDetail/$nodeHashId"
        val msg = if (nodeHashId.isNullOrBlank()) {
            ""
        } else {
            " <a target='_blank' href='$link'>$agentHashId</a>"
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

    /**
     * 给引擎写入排队的启停时间，时间为 millis 的 timestamp
     */
    fun updateQueueTime(data: ThirdPartyAgentDispatchData, createTime: Long?, endTime: Long?) {
        updateQueueTime(
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            buildId = data.buildId,
            vmSeqId = data.vmSeqId,
            executeCount = data.executeCount ?: 1,
            createTime = createTime,
            endTime = endTime
        )
    }

    fun updateQueueTime(event: PipelineAgentStartupEvent, createTime: Long?, endTime: Long?) {
        updateQueueTime(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            executeCount = event.executeCount ?: 1,
            createTime = createTime,
            endTime = endTime
        )
    }

    fun updateQueueTime(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        createTime: Long?,
        endTime: Long?
    ) {
        // TODO: #9897 因为需要前端配合，所以一期先不写入耗时，等待前端完善排队耗时展示
        try {
            client.get(ServiceBuildResource::class).updateContainerTimeout(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = vmSeqId,
                executeCount = executeCount,
                timestamps = mapOf(
                    BuildTimestampType.JOB_THIRD_PARTY_QUEUE to BuildRecordTimeStamp(createTime, endTime)
                )
            )
        } catch (e: Throwable) {
            logger.error("updateQueueTime|$projectId|$pipelineId|$buildId|$vmSeqId|$executeCount" +
                    "|$createTime|$endTime|error", e)
        }
    }

    companion object {
        fun queueRetry(
            errorCode: ErrorCodeEnum,
            errMsg: String? = null,
            suffixMsg: String = ""
        ): DispatchRetryMQException {
            throw DispatchRetryMQException(
                errorCodeEnum = errorCode,
                errorMessage = (errMsg ?: errorCode.getErrorMessage()) + suffixMsg
            )
        }

        fun queueFailureI18n(
            errorCode: ErrorCodeEnum,
            messageCode: String? = null,
            param: Array<String>? = null
        ): BuildFailureException {
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
        ): BuildFailureException {
            return BuildFailureException(
                errorType = errorCode.errorType,
                errorCode = errorCode.errorCode,
                formatErrorMessage = errorCode.formatErrorMessage,
                errorMessage = errMsg
            )
        }

        private const val TPA_QUEUE_LOG_TAG = "tpa_queue_log_tag"

        // 打印带特定tag的日志
        fun Logger.tagError(msg: String) = this.error("$TPA_QUEUE_LOG_TAG$msg")
        fun Logger.tagError(msg: String, o: Any) = this.error("$TPA_QUEUE_LOG_TAG$msg", o)

        private val logger = LoggerFactory.getLogger(TPACommonUtil::class.java)
    }
}
