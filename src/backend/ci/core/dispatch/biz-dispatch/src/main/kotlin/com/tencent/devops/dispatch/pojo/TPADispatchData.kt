package com.tencent.devops.dispatch.pojo

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType

data class ThirdPartyAgentDispatchData(
    val id: String,
    val secretKey: String,
    val userId: String,
    val projectId: String,
    val pipelineId: String,
    val pipelineName: String,
    val buildId: String,
    val buildNo: Int,
    val os: String,
    val vmSeqId: String,
    val taskName: String,
    val channelCode: String,
    val atoms: Map<String, String>,
    val containerHashId: String?,
    val executeCount: Int?,
    val jobId: String?,
    val queueTimeoutMinutes: Int?,
    val dispatchType: ThirdPartyAgentDispatch,
    val ignoreEnvAgentIds: Set<String>?,
    val singleNodeConcurrency: Int?,
    val allNodeConcurrency: Int?
) {
    fun isEnv() = dispatchType.isEnv()
    // 生成环境资源标识，需要添加共享项目信息
    fun genEnvWithProject(): String? {
        if (dispatchType !is ThirdPartyAgentEnvDispatchType) {
            return null
        }
        return dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }?.let { "$it@${dispatchType.envName}" }
            ?: dispatchType.envName
    }

    fun genEnv(): String? {
        if (dispatchType !is ThirdPartyAgentEnvDispatchType) {
            return null
        }
        return dispatchType.envName
    }

    fun isSingle() = dispatchType.isSingle()
    // 生成Agent资源标识
    fun genAgent(): String? {
        if (dispatchType !is ThirdPartyAgentIDDispatchType) {
            return null
        }
        return dispatchType.displayName
    }

    // 方便打印日志
    fun toLog(): String {
        var msg = "$userId|$projectId|$pipelineId|$buildId|$vmSeqId"
        msg += when (dispatchType) {
            is ThirdPartyAgentEnvDispatchType -> "|env=${this.genEnvWithProject()}"
            is ThirdPartyAgentIDDispatchType -> "|agent=${this.genAgent()}"
            else -> ""
        }
        return msg
    }

    constructor(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentDispatch
    ) : this(
        id = dispatchMessage.id,
        secretKey = dispatchMessage.secretKey,
        userId = dispatchMessage.event.userId,
        projectId = dispatchMessage.event.projectId,
        pipelineId = dispatchMessage.event.pipelineId,
        pipelineName = dispatchMessage.event.pipelineName,
        buildId = dispatchMessage.event.buildId,
        buildNo = dispatchMessage.event.buildNo,
        os = dispatchMessage.event.os,
        vmSeqId = dispatchMessage.event.vmSeqId,
        taskName = dispatchMessage.event.taskName,
        channelCode = dispatchMessage.event.channelCode,
        atoms = dispatchMessage.event.atoms,
        containerHashId = dispatchMessage.event.containerHashId,
        executeCount = dispatchMessage.event.executeCount,
        jobId = dispatchMessage.event.jobId,
        queueTimeoutMinutes = dispatchMessage.event.queueTimeoutMinutes,
        dispatchType = dispatchType,
        ignoreEnvAgentIds = dispatchMessage.event.ignoreEnvAgentIds,
        singleNodeConcurrency = dispatchMessage.event.singleNodeConcurrency,
        allNodeConcurrency = dispatchMessage.event.allNodeConcurrency
    )

    constructor(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        infoData: ThirdPartyAgentDispatchDataSqlJson
    ) : this(
        id = infoData.id,
        secretKey = infoData.secretKey,
        userId = infoData.userId,
        projectId = projectId,
        pipelineId = pipelineId,
        pipelineName = infoData.pipelineName,
        buildId = buildId,
        buildNo = infoData.buildNo,
        os = infoData.os,
        vmSeqId = vmSeqId,
        taskName = infoData.taskName,
        channelCode = infoData.channelCode,
        atoms = infoData.atoms,
        containerHashId = infoData.containerHashId,
        executeCount = infoData.executeCount,
        jobId = infoData.jobId,
        queueTimeoutMinutes = infoData.queueTimeoutMinutes,
        dispatchType = infoData.dispatchType,
        ignoreEnvAgentIds = infoData.ignoreEnvAgentIds,
        singleNodeConcurrency = infoData.singleNodeConcurrency,
        allNodeConcurrency = infoData.allNodeConcurrency
    )

    fun genSqlJsonData(): ThirdPartyAgentDispatchDataSqlJson {
        return ThirdPartyAgentDispatchDataSqlJson(
            id = this.id,
            secretKey = this.secretKey,
            userId = this.userId,
            pipelineName = this.pipelineName,
            buildNo = this.buildNo,
            os = this.os,
            taskName = this.taskName,
            channelCode = this.channelCode,
            atoms = this.atoms,
            containerHashId = this.containerHashId,
            executeCount = this.executeCount,
            jobId = this.jobId,
            queueTimeoutMinutes = this.queueTimeoutMinutes,
            dispatchType = this.dispatchType,
            ignoreEnvAgentIds = this.ignoreEnvAgentIds,
            singleNodeConcurrency = this.singleNodeConcurrency,
            allNodeConcurrency = this.allNodeConcurrency
        )
    }
}

// 保存到数据库中的 json 类型
data class ThirdPartyAgentDispatchDataSqlJson(
    val id: String,
    val secretKey: String,
    val userId: String,
    val pipelineName: String,
    val buildNo: Int,
    val os: String,
    val taskName: String,
    val channelCode: String,
    val atoms: Map<String, String>,
    val containerHashId: String?,
    val executeCount: Int?,
    val jobId: String?,
    val queueTimeoutMinutes: Int?,
    val dispatchType: ThirdPartyAgentDispatch,
    val ignoreEnvAgentIds: Set<String>?,
    val singleNodeConcurrency: Int?,
    val allNodeConcurrency: Int?
)

// 数据库使用的排队类型
enum class ThirdPartyAgentSqlQueueType {
    AGENT,
    ENV;
}
