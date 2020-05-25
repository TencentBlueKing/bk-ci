package com.tencent.devops.process.service

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PipelineQuotaService @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    companion object {
        private const val QUOTA_KEY_PREFIX = "project_quota_key_"
        private const val QUOTA_KEY_LIMIT_PREFIX = "project_quota_limit_key_"
        private const val QUOTA_BAD_PROJECT_ALL_KEY = "project_quota_all_key" // 所有异常项目集合
        private const val SEVEN_DAY_MILL_SECONDS = 3600 * 24 * 7 * 1000L
        private const val DEFAULT_PROJECT_QUOTA = 1000L
        private const val LOCK_KEY = "pipeline_quota_redis_lock"
    }

    private val logger = LoggerFactory.getLogger(PipelineQuotaService::class.java)

    // 配额就是带projectId集合元素个数的汇总
    fun getProjectRemainQuota(projectId: String): Pair<Long, Long> {
        try {
            val quota = getQuotaByProject(projectId)
            val usedQuota = getUsedQuota(projectId)
            return Pair(quota - usedQuota, quota)
        } catch (e: Exception) {
            logger.error("fail to get quota by project: $projectId", e)
        }
        return Pair(DEFAULT_PROJECT_QUOTA, DEFAULT_PROJECT_QUOTA)
    }

    private fun getUsedQuota(projectId: String): Long {
        return redisOperation.zsize(
            key = getProjectKey(projectId),
            min = (System.currentTimeMillis() - SEVEN_DAY_MILL_SECONDS).toDouble(),
            max = System.currentTimeMillis().toDouble()
        )
    }

    fun incQuotaByProject(projectId: String, buildId: String, jobId: String) {
        try {
            val value = getProjectJobKey(projectId, buildId, jobId)
            redisOperation.sadd(QUOTA_BAD_PROJECT_ALL_KEY, value)
            redisOperation.zadd(getProjectKey(projectId), value, System.currentTimeMillis().toDouble())
        } catch (e: Exception) {
            logger.error("fail to inc quota by project: $projectId", e)
        }
    }

    fun decQuotaByProject(projectId: String, buildId: String, jobId: String) {
        try {
            val value = getProjectJobKey(projectId, buildId, jobId)
            redisOperation.sremove(QUOTA_BAD_PROJECT_ALL_KEY, value)
            redisOperation.zremove(getProjectKey(projectId), value)
        } catch (e: Exception) {
            logger.error("fail to dec quota by project: $projectId", e)
        }
    }

    fun getQuotaByProject(projectId: String): Long {
        return redisOperation.get(getProjectLimitKey(projectId))?.toLong() ?: DEFAULT_PROJECT_QUOTA
    }

    fun setQuotaByProject(projectId: String, quota: Long) {
        redisOperation.set(getProjectLimitKey(projectId), quota.toString())
    }

    @Scheduled(cron = "0 0 0 * * ?")
    fun clearZSet() {
        logger.info("try to start to clear quota zset")
        val redisLock = RedisLock(redisOperation, LOCK_KEY, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("<<< Clear Pipeline Quota Start >>>")
                doClear()
            } else {
                logger.info("<<< Clear Pipeline Quota Job Has Running, Do Not Start>>>")
            }
        } catch (e: Throwable) {
            logger.error("Clear pipeline quota exception:", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun doClear() {
        val min = 0.0
        val max = (System.currentTimeMillis() - SEVEN_DAY_MILL_SECONDS).toDouble()
        val removeKey = mutableSetOf<String>()

        // 清理之前没释放的配额
        redisOperation.sscan(QUOTA_BAD_PROJECT_ALL_KEY, "*")?.use { cursor ->
            while (cursor.hasNext()) {
                val it = cursor.next() // ${projectId}_${buildId}_${jobId}
                val arr = it.split("_")
                val projectId = arr[0]
                val count = redisOperation.zremoveRangeByScore(getProjectKey(projectId), min, max)
                removeKey.add(it)
                logger.info("success remove record for project: $projectId, $count")
            }
        }

        removeKey.forEach { redisOperation.sremove(QUOTA_BAD_PROJECT_ALL_KEY, it) }

        logger.info("finish to clear zset")
    }

    private fun getProjectLimitKey(projectId: String): String {
        return "$QUOTA_KEY_LIMIT_PREFIX$projectId"
    }

    private fun getProjectKey(projectId: String): String {
        return "$QUOTA_KEY_PREFIX$projectId"
    }

    private fun getProjectJobKey(projectId: String, buildId: String, jobId: String): String {
        return "${projectId}_${buildId}_$jobId"
    }
}