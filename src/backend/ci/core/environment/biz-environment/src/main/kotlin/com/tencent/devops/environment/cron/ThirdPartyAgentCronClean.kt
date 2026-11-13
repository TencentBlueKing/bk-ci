package com.tencent.devops.environment.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentCronClean @Autowired constructor(
    private val dslContext: DSLContext,
    private val actionDao: ThirdPartyAgentActionDao,
    private val redisOperation: RedisOperation
) {
    // 清理超过100的Action数据
    @Scheduled(cron = "0 28 3 * * ?")
    fun cronDeleteActionRecord() {
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = LOCK_KEY,
            expiredTimeInSeconds = LOCK_EXPIRED_SECONDS
        )
        if (!lock.tryLock()) {
            logger.info("Skip third-party Agent action cleanup because the lock was not acquired")
            return
        }
        lock.use {
            deleteOldActionRecords()
        }
    }

    private fun deleteOldActionRecords() {
        val agentIds = actionDao.fetchAgentIdByGtCount(dslContext, 100)
        agentIds.forEach { agentId ->
            logger.info("cronDeleteActionRecord start clean agent $agentId")

            val id = actionDao.getIndexId(dslContext, agentId, 1, 99) ?: run {
                logger.info("cronDeleteActionRecord agent $agentId index 100 is null")
                return@forEach
            }
            logger.info("cronDeleteActionRecord agent $agentId will clean less than id $id")

            val count = actionDao.deleteOldActionById(dslContext, agentId, id)
            logger.info("cronDeleteActionRecord agent $agentId cleaned $count")

            Thread.sleep(500)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentCronClean::class.java)
        private const val LOCK_KEY = "env_cron_thirdPartyAgentActionClean"
        private const val LOCK_EXPIRED_SECONDS = 3600L
    }
}
