package com.tencent.devops.process.service

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class PipelineQuotaService @Autowired constructor(
    private val redisOperation: RedisOperation
){
    companion object {
        private const val QUOTA_KEY_PREFIX = "project_quota_key_"
        private const val QUOTA_USED_KEY_PREFIX = "project_quota_used_key_"
    }
    private val logger = LoggerFactory.getLogger(PipelineQuotaService::class.java)

    // 配额就是带projectId集合元素个数的汇总
    fun getQuotaByProject(projectId: String): Long {
        try {
            val quota = redisOperation.get(getProjectKey(projectId))?.toLong() ?: Long.MAX_VALUE
            val usedQuota = redisOperation.get(getProjectUsedKey(projectId))?.toLong() ?: 0
            return quota - usedQuota
        } catch (e: Exception) {
            logger.error("fail to get quota by project: $projectId", e)
        }
        return Long.MAX_VALUE
    }

    fun incQuotaByProject(projectId: String) {
        try {
            redisOperation.increment(getProjectUsedKey(projectId), 1)
            redisOperation.expireAt(getProjectUsedKey(projectId), getNextMonth())
        } catch (e: Exception) {
            logger.error("fail to inc quota by project: $projectId", e)
        }
    }

    private fun getNextMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.time
    }

    fun setQuotaByProject(projectId: String, quota: Long) {
        redisOperation.set(getProjectKey(projectId), quota.toString())
    }

    fun setQuotaUsedByProject(projectId: String, usedQuota: Long) {
        redisOperation.set(getProjectUsedKey(projectId), usedQuota.toString())
    }

    private fun getProjectUsedKey(projectId: String): String {
        return "$QUOTA_USED_KEY_PREFIX${projectId}"
    }

    private fun getProjectKey(projectId: String): String {
        return "$QUOTA_KEY_PREFIX${projectId}"
    }
}