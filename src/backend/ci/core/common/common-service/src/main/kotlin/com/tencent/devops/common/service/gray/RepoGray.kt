package com.tencent.devops.common.service.gray

import com.tencent.devops.common.redis.RedisOperation

class RepoGray {
    companion object {
        const val repoGrayRedisKey = "project:setting:repoGray"
    }

    fun isGray(projectId: String, redisOperation: RedisOperation): Boolean {
        return grayProjectSet(redisOperation).contains(projectId)
    }

    fun grayProjectSet(redisOperation: RedisOperation) =
        (redisOperation.getSetMembers(repoGrayRedisKey) ?: emptySet()).filter { !it.isBlank() }.toSet()
}
