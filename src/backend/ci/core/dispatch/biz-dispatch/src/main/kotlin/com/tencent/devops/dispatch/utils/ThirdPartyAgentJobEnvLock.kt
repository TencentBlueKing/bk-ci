package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

/**
 * Job 配额按构建项目 + 流水线 + Job + 环境统计，不按环境所属项目统计。
 * 普通路径：环境锁 -> Job 锁 -> agent 锁；细粒度路径：Job 锁 -> agent 锁。
 * 两条路径始终使用同一 Job 锁，防止灰度开关切换期间的在途请求绕过彼此。
 * 这里保留无配额 Job 的锁，避免同一 Job 不同构建的配额配置不一致时失去互斥。
 */
class ThirdPartyAgentJobEnvLock(
    redisOperation: RedisOperation,
    projectId: String,
    pipelineId: String,
    jobId: String,
    envId: Long
) : RedisLock(redisOperation, key(projectId, pipelineId, jobId, envId), 60L) {
    companion object {
        internal fun key(projectId: String, pipelineId: String, jobId: String, envId: Long): String =
            "DISPATCH_REDIS_LOCK_JOB_ENV_" + listOf(projectId, pipelineId, jobId, envId.toString())
                .joinToString(":") { "${it.length}:$it" }
    }
}
