package com.tencent.devops.common.pipeline.container

import com.tencent.devops.common.pipeline.type.agent.AgentType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * AgentReuseMutex 构建机复用互斥
 * 在同一次构建中总共有五种情况
 * 1、单个Agent节点且明确指定了使用某个Agent(AgentId或者AgentName)，这种情况在item中可以直接拿到，直接进行项目级的互斥逻辑
 * 2、单个Agent节点但使用变量指代Agent
 *   2-1、被指代的Agent节点与复用他的Job处在同一Stage下，并行执行，这时被指代的Agent与复用他的Job进行同样的变量替换，
 *   并行竞争，没有先后顺序
 *   2-2、被指代的Agent节点与复用他的Job处在同一Stage下，串行执行(即流水线的Job依赖)，这时被指代的Agent应先于复用他的Job执行，
 *   且在Var表中写入可以拿到Agent的只读变量，复用Job读取变量执行
 * 3、Agent集群且明确指定了某个集群(集群ID或者名称)，在分发到Dispatch前需要进行集群级别的互斥锁逻辑，
 * 这样可以保证同样进行互斥逻辑的集群在引擎处进行等待。
 * 与单个Agent节点的Job关系相同，并行执行时并行竞争，串行时天然的有先后顺序，读取Var表中节点进行项目级的互斥逻辑
 * 4、Agent集群但使用变量指代，与未进行变量指代的逻辑一致，无需特殊声明
 *
 * 注：如果是共享集群不受影响，依旧使用执行项目来进行锁定
 *
 * @param reUseJobId 如果为空说明当前job为被依赖job
 * @param agentOrEnvId agentId/agentName 或者 envId/envName 或者变量，根据 type 区分不同值
 * @param type 复用互斥的各种类型
 * @param endJob 是否是最后一波的Job
 */
@Schema(title = "构建机复用互斥模型")
data class AgentReuseMutex(
    val jobId: String,
    val reUseJobId: String?,
    val agentOrEnvId: String?,
    val type: AgentReuseMutexType,
    val endJob: Boolean,
    @get:Schema(title = "是否排队", required = false)
    val queueEnable: Boolean = true,
    @get:Schema(title = "排队等待时间（分钟）0表示不等待直接失败", required = false)
    var timeout: Int = 900,
    @get:Schema(title = "支持变量解析的timeout，变量值非数字则会改取timeout值", required = false)
    var timeoutVar: String? = null,
    @get:Schema(title = "排队队列大小", required = false)
    val queue: Int = 10,
    // 运行时的agentOrEnvId，如果有值说明已经初始化了
    var runtimeAgentOrEnvId: String? = null,
    @get:Schema(title = "占用锁定的信息用于日志提示", required = false)
    var linkTip: String? = null // 占用锁定的信息用于日志提示/不写入到Model，仅在构建开始时产生
) {
    companion object {
        /**
         * @see com.tencent.devops.process.engine.common.Timeout MAX_MINUTES
         * JOB最长过期时间 7 天对应的秒数
         */
        const val AGENT_LOCK_TIMEOUT = 60 * 60 * 24 * 7
        const val CONTEXT_KEY_SUFFIX = ".container.agent_id"
        fun genAgentContextKey(jobId: String) = "jobs.$jobId$CONTEXT_KEY_SUFFIX"
        fun genAgentReuseMutexLockKey(projectId: String, agentId: String) =
            "lock:agent:reuse:project:$projectId:agent:$agentId:lock"

        fun genAgentReuseMutexQueueKey(projectId: String, agentId: String) =
            "lock:agent:reuse:project:$projectId:agent:$agentId:queue"

        fun genAgentReuseMutexLinkTipKey(buildId: String): String {
            return "linkTip:$buildId"
        }
    }
}

enum class AgentReuseMutexType {
    // 可以直接拿到AgentId，可能是固定的或者变量，直接使用或者替换即可
    AGENT_ID,

    // 可以直接拿到AgentName，可能是固定的或者变量，直接使用或者替换后查询Env接口换成AgentId使用
    AGENT_NAME,

    // Agent环境ID或变量，直接使用或者替换，取环境锁使用
    AGENT_ENV_ID,

    // Agent环境NAME或变量，直接使用或者替换后查询Env接口换成环境ID，取环境锁使用
    AGENT_ENV_NAME,

    // 位于被依赖Job后执行的Job，可以直接从Var表中通过 reUseJobId 读取AgentId，jobs.<jobId>.container.node_alias
    AGENT_DEP_VAR;

    fun isEnvType() = (this == AGENT_ENV_ID || this == AGENT_ENV_NAME)
    fun isAgentType() = (this == AGENT_ID || this == AGENT_NAME)
    fun needEngineLock() = isAgentType()

    fun toAgentType(): AgentType? = when (this) {
        AGENT_ID, AGENT_ENV_ID -> AgentType.ID
        AGENT_NAME, AGENT_ENV_NAME -> AgentType.NAME
        else -> null
    }
}
