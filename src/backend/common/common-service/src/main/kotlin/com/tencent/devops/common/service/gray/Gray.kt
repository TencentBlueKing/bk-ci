package com.tencent.devops.common.service.gray

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Gray {

    @Value("\${project.gray:#{null}}")
    private val grayFlag: String? = null

    var gray: Boolean? = null
    private val redisKey = "project:setting:gray" // 灰度项目列表存在redis的标识key

    fun isGray(): Boolean {
        if (gray == null) {
            synchronized(this) {
                if (gray == null) {
                    gray = !grayFlag.isNullOrBlank() && grayFlag!!.toBoolean()
                }
            }
        }
        return gray!!
    }

    fun isGrayMatchProject(projectId: String, redisOperation: RedisOperation): Boolean {
        return isGrayMatchProject(projectId, grayProjectSet(redisOperation))
    }

    fun isGrayMatchProject(projectId: String, grayProjectSet: Set<String>): Boolean {
        return isGray() == grayProjectSet.contains(projectId)
    }

    fun grayProjectSet(redisOperation: RedisOperation) =
        (redisOperation.getSetMembers(getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()

    fun getGrayRedisKey() = redisKey
}
