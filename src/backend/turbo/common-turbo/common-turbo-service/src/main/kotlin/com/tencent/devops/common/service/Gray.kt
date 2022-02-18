package com.tencent.devops.common.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

class Gray {

    @Value("\${project.gray.v2:#{null}}")
    private val grayFlag: String? = "false"

    var gray: Boolean? = null

    private val redisKey = "project:setting:gray:v2" // v2灰度项目列表存在redis的标识key

    private val cache = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<String/*Redis Keys*/, Set<String>/*Project Names*/>()

    fun isGray(): Boolean {
        if (gray == null) {
            synchronized(this) {
                if (gray == null) {
                    gray = !grayFlag.isNullOrBlank() && grayFlag.toBoolean()
                }
            }
        }
        return gray!!
    }

    fun isGrayProject(projectId: String, redisOperation: RedisOperation): Boolean? {
        return redisOperation.isMember(getGrayRedisKey(), projectId)
//        return grayProjectSet(redisOperation).contains(projectId)
    }

    fun isGrayMatchProject(projectId: String, redisOperation: RedisOperation): Boolean {
        return isGray() == isGrayProject(projectId, redisOperation) // 当前是灰度环境 + 灰度项目
    }

//    fun isGrayMatchProject(projectId: String, grayProjectSet: Set<String>): Boolean {
//        return isGray() == grayProjectSet.contains(projectId)
//    }

    fun grayProjectSet(redisOperation: RedisOperation): Set<String> {
        var projects = cache.getIfPresent(getGrayRedisKey())
        if (projects != null) {
            return projects
        }
        synchronized(this) {
            projects = cache.getIfPresent(getGrayRedisKey())
            if (projects != null) {
                return projects!!
            }
            logger.info("Refresh the local gray projects")
            projects = (redisOperation.getSetMembers(getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()
            cache.put(getGrayRedisKey(), projects!!)
        }
        return projects!!
    }

    fun getGrayRedisKey() = redisKey

    companion object {
        private val logger = LoggerFactory.getLogger(Gray::class.java)
    }
}
