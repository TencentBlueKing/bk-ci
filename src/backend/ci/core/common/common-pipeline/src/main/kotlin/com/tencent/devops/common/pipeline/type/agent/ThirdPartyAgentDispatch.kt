package com.tencent.devops.common.pipeline.type.agent

import com.tencent.devops.common.pipeline.type.DispatchType

/**
 * 第三方构建机类型抽象父类，方便泛型处理
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ThirdPartyAgentDispatch(
    override var value: String,
    open val agentType: AgentType,
    // 类型为REUSE_JOB时，被复用的job的value，防止同一个stage并发下拿不到agent，启动时填充
    open var reusedInfo: ReusedInfo?
) : DispatchType(value) {
    fun idType(): Boolean = (agentType == AgentType.ID) || (reusedInfo?.agentType == AgentType.ID)

    // 是否在复用锁定链上
    fun hasReuseMutex(): Boolean = this.agentType.isReuse() || this.reusedInfo != null
}

/**
 * 被复用对象的信息
 * @param value 被复用Job的值
 * @param agentType 被复用Job的类型
 * @param jobId 非根节点且被复用节点所复用的根jobId
 */
data class ReusedInfo(
    val value: String,
    val agentType: AgentType,
    val jobId: String?
)
