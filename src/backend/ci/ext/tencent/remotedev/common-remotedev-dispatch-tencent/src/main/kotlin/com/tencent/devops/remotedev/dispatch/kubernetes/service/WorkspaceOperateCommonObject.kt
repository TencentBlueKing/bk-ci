package com.tencent.devops.remotedev.dispatch.kubernetes.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation

/**
 * 两个模块可以同时引用的公共操作
 */
object WorkspaceOperateCommonObject {
    fun saveRebuildOptions(
        redisOperation: RedisOperation,
        taskUid: String,
        data: RebuildOptions
    ) {
        redisOperation.set(
            key = genRebuildOptionsKey(taskUid),
            value = JsonUtil.toJson(data, false),
            // 默认7天过期
            expiredInSecond = 60 * 60 * 24 * 7,
            expired = true
        )
    }

    fun getRebuildOptions(
        redisOperation: RedisOperation,
        taskUid: String
    ): RebuildOptions? {
        val dataStr = redisOperation.get(genRebuildOptionsKey(taskUid))?.ifBlank { null } ?: return null
        return JsonUtil.to(dataStr, object : TypeReference<RebuildOptions>() {})
    }

    fun deleteRebuildOptions(
        redisOperation: RedisOperation,
        taskUid: String
    ) {
        redisOperation.delete(genRebuildOptionsKey(taskUid))
    }

    private fun genRebuildOptionsKey(taskUid: String) =
        "$REBUILD_OPTIONS_REDIS_KEY_PRI:$taskUid"

    private const val REBUILD_OPTIONS_REDIS_KEY_PRI = "remotedev:workspace:rebuild"
}

data class RebuildOptions(
    val removeOwner: Boolean
)