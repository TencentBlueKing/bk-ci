package com.tencent.devops.dispatch.devcloud.utils

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DevCloudJobRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    fun setJobCount(buildId: String, containerName: String) {
        redisOperation.increment("$buildId-$containerName", 1)
    }

    fun getJobCount(buildId: String, containerName: String): Int {
        val jobCount = redisOperation.get("$buildId-$containerName")
        return jobCount?.toInt() ?: 0
    }

    fun deleteJobCount(buildId: String, containerName: String) {
        redisOperation.delete("$buildId-$containerName")
    }
}
