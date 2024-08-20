package com.tencent.devops.dispatch.pojo

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import java.time.LocalDateTime

@Event(MQ.EXCHANGE_THIRD_PARTY_AGENT_QUEUE, MQ.ROUTE_THIRD_PARTY_AGENT_QUEUE)
data class TPAQueueEvent(
    val projectId: String,
    val pipelineId: String,
    val data: String,
    val dataType: ThirdPartyAgentSqlQueueType,
    // 发送这个事件的消息，只在第一次发送消息时携带，重放后置空，方便做一些日志输出或者记录
    var sendData: ThirdPartyAgentDispatchData?,
    // 消息队列延迟消息时间
    val delayMills: Int,
    // 拿到的锁的值，为了保证生产和消费始终只有一个消息，所以需要在生产和消费端共用一把锁
    val lockValue: String
) {
    fun toLog() = "${this.projectId}|${this.pipelineId}|${this.data}|${this.dataType}"
}

data class TPAQueueEventContext(
    var context: EnvQueueContext? = null,
    var needDeleteRecord: Pair<Long, Long>? = null,
    val needRetryRecord: MutableSet<Long> = mutableSetOf(),
    val startTimeMilliSecond: Long = System.currentTimeMillis()
) {
    fun setDelete(recordId: Long) {
        needDeleteRecord = Pair(recordId, LocalDateTime.now().timestampmilli())
    }

    fun addRetry(recordId: Long) {
        needRetryRecord.add(recordId)
    }
}

/**
 * 用来存放构建机通用场景的上下文，非线程安全
 * 生命周期更随整个队列
 */
open class QueueContext(
    open var envId: Long?
)

/**
 * 用来存放构建机环境队列中所有消息检查共用的上下文，非线程安全。需要保证每个动态获取的校验数据在这一个校验组内都是相同的
 * 生命周期更随整个队列
 * @param envId 环境ID，调度环境类型时生效
 * @param agents 当前可以使用的 agent 列表，可能会随着不同消息经历的操作不同而发生改变
 * @param projectJobRunningAndQueueAllCount 流水线下这个Job上节点运行的所有任务数量，根据jobId区分缓存
 * @param agentsJobRunningAndQueueAllMap 流水线下这个Job上各个节点运行的任务数量，会随着 agent 列表发生改变而改变，根据jobId区分缓存
 * @param agentRunningCnt 每个节点上正在跑的任务数量，会随着 agent 列表发生改变而改变
 * @param dockerRunningCnt 每个节点上正在跑的docker任务数量，会随着 agent 列表发生改变而改变
 * @param hasTryAgents 已经尝试过不行的 agent，每个队列只尝试一次
 */
data class EnvQueueContext(
    override var envId: Long?,
    var agents: List<ThirdPartyAgent>,
    // AllNodeConcurrencyCheck
    private val projectJobRunningAndQueueAllCount: MutableMap<String, Long> = mutableMapOf(),
    // SingleNodeConcurrencyCheck
    private val agentsJobRunningAndQueueAllMap: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(),
    // PickupAgentCheck
    val agentRunningCnt: MutableMap<String, Int> = mutableMapOf(),
    val dockerRunningCnt: MutableMap<String, Int> = mutableMapOf(),
    val hasTryAgents: MutableSet<String> = mutableSetOf()
) : QueueContext(envId = envId) {
    fun projectJobRunningAndQueueAllCount(jobId: String): Long? {
        return projectJobRunningAndQueueAllCount[jobId]
    }

    fun setProjectJobRunningAndQueueAllCount(jobId: String, cnt: Long?) {
        if (cnt == null) {
            projectJobRunningAndQueueAllCount.remove(jobId)
            return
        }
        projectJobRunningAndQueueAllCount[jobId] = cnt
    }

    fun agentsJobRunningAndQueueAllMap(jobId: String): MutableMap<String, Int>? {
        return agentsJobRunningAndQueueAllMap[jobId]
    }

    fun setAgentsJobRunningAndQueueAllMap(jobId: String, agentId: String, cnt: Int?) {
        if (cnt == null) {
            agentsJobRunningAndQueueAllMap[jobId]?.remove(agentId)
            return
        }
        agentsJobRunningAndQueueAllMap[jobId]?.set(agentId, cnt)
    }

    fun setAllAgentsJobRunningAndQueueAllMap(jobId: String, map: MutableMap<String, Int>) {
        agentsJobRunningAndQueueAllMap[jobId] = map
    }
}

/**
 * 队列中单独消息自己的上下文，生命周期跟随每次消息执行
 * @param data 消息详情
 * @param retryTime 重试次数
 * @param buildAgent 这个消息每次选择的尝试去下发任务的 agent
 */
data class QueueDataContext(
    val data: ThirdPartyAgentDispatchData,
    val retryTime: Int,
    // GenAgentBuildCheck
    var buildAgent: ThirdPartyAgent? = null
) {
    fun retryLog(msg: String?) = " - retry $retryTime: $msg"
}

class QueueRetryException(
    errorCodeEnum: ErrorCodeEnum,
    errorMessage: String?
) : ErrorCodeException(
    errorCode = errorCodeEnum.errorCode.toString(),
    errorType = errorCodeEnum.errorType,
    defaultMessage = errorMessage
)

open class QueueFailureException(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String,
    errorMessage: String
) : Exception(errorMessage)