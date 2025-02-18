package com.tencent.devops.common.pipeline.type.agent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.common.pipeline.type.DispatchType

/**
 * 第三方构建机类型抽象父类，方便泛型处理
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ThirdPartyAgentDispatch(
    override var value: String,
    open val agentType: AgentType,
    open var workspace: String?,
    // 第三方构建机用docker作为构建机
    open val dockerInfo: ThirdPartyAgentDockerInfo?,
    // 类型为REUSE_JOB时，被复用的job的value，防止同一个stage并发下拿不到agent，启动时填充
    open var reusedInfo: ReusedInfo?
) : DispatchType(value) {
    // 本身是 id，和被复用对象在同一JOB且被复用对象也是 id，是复用但是位于后面的 JOB
    fun idType(): Boolean =
        (agentType == AgentType.ID) || (reusedInfo?.agentType == AgentType.ID) ||
                (agentType == AgentType.REUSE_JOB_ID && reusedInfo == null)

    // 是否在复用锁定链上
    fun hasReuseMutex(): Boolean = this.agentType.isReuse() || this.reusedInfo != null

    fun isEnv() = this is ThirdPartyAgentEnvDispatchType
    fun isSingle() = this is ThirdPartyAgentIDDispatchType
}

/**
 * 被复用对象的信息
 * @param value 被复用Job的值
 * @param agentType 被复用Job的类型
 * @param jobId 非根节点且被复用节点所复用的根jobId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReusedInfo(
    val value: String,
    val agentType: AgentType,
    val jobId: String?
)
