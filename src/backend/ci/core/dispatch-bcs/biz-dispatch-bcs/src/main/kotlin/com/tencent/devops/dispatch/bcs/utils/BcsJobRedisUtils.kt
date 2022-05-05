package com.tencent.devops.dispatch.bcs.utils

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BcsJobRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    // TODO: 这里要问下左涛看会不会和devcloud冲突
    fun setJobCount(buildId: String, builderName: String) {
        redisOperation.increment("$buildId-$builderName", 1)
    }

    fun getJobCount(buildId: String, builderName: String): Int {
        val jobCount = redisOperation.get("$buildId-$builderName")
        return jobCount?.toInt() ?: 0
    }

    fun deleteJobCount(buildId: String, builderName: String) {
        redisOperation.delete("$buildId-$builderName")
    }
}
