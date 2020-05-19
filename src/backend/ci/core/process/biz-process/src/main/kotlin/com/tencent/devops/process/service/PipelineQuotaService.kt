package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PipelineQuotaService @Autowired constructor(
    private val redisOperation: RedisOperation
){
    companion object {
        private const val QUOTA_LIMIT_KEY_PREFIX = "project_quota_limit_"
        private const val QUOTA_BUILD_KEY_PREFIX = "project_quota_build_key_"
    }
    private val logger = LoggerFactory.getLogger(PipelineQuotaService::class.java)

    // 配额就是带projectId集合元素个数的汇总
    fun getQuotaByProject(projectId: String): Long {
        val quotaLimit = redisOperation.get(getProjectLimitKey(projectId))?.toLong() ?: Long.MAX_VALUE

        var count = 0L
        redisOperation.sscan("$QUOTA_LIMIT_KEY_PREFIX${projectId}").open().use { cursor ->
            while (cursor.hasNext()) {
                val set = cursor.next()
                count += set.length
            }
        }

        return quotaLimit - count
    }

    fun decQuotaByProject(projectId: String, buildId: String) {
        try {
            redisOperation.spop(getProjectBuildKey(projectId, buildId))
        } catch (e: Exception) {
            logger.error("fail to dec quota by project", e)
        }
    }

    fun incQuotaByProject(projectId: String, buildId: String) {
        try {
            redisOperation.sadd(getProjectBuildKey(projectId, buildId), UUIDUtil.generate())
            // 每个job构建最多8小时，过期释放
            redisOperation.expire(getProjectBuildKey(projectId, buildId), 8, TimeUnit.HOURS)
        } catch (e: Exception) {
            logger.error("fail to inc quota by project", e)
        }
    }

    fun setQuotaByProject(projectId: String, quota: Long) {
        redisOperation.set(getProjectLimitKey(projectId), quota.toString())
    }

    // projectId和buildId构成一个集合
    private fun getProjectBuildKey(projectId: String, buildId: String): String {
        return "$QUOTA_BUILD_KEY_PREFIX${projectId}_${buildId}"
    }

    private fun getProjectLimitKey(projectId: String): String {
        return "$QUOTA_LIMIT_KEY_PREFIX${projectId}"
    }
}