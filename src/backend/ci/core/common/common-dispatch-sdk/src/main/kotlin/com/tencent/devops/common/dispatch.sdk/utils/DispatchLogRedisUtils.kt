package com.tencent.devops.common.dispatch.sdk.utils

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.common.Timeout

object DispatchLogRedisUtils {

    private fun redisOperation() = SpringContextUtil.getBean(RedisOperation::class.java)

    fun setRedisExecuteCount(buildId: String, executeCount: Int?) {
        val key = getKey(buildId)
        redisOperation().set(key, executeCount?.toString() ?: "1", Timeout.transMinuteTimeoutToSec(null))
    }

    fun removeRedisExecuteCount(buildId: String) {
        val key = getKey(buildId)
        redisOperation().delete(key)
    }

    /**
     *  @param buildId 在一个时间点只能执行一次
     */
    private fun getKey(buildId: String): String {
        return "dispatch_log:$buildId"
    }

    fun getRedisExecuteCount(buildId: String): Int {
        return redisOperation().get(getKey(buildId))?.toIntOrNull() ?: 1
    }
}
