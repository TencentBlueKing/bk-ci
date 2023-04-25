package com.tencent.devops.artifactory.cron

import com.tencent.devops.artifactory.dao.ShortUrlDao
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
class ShortUrlCleanJob(
    private val dslContext: DSLContext,
    private val shortUrlDao: ShortUrlDao,
    private val redisOperation: RedisOperation
) {

    @Scheduled(cron = "0 15 0 * * ? ")
    fun cleanExpiredShortUrl() {
        val redisLock = RedisLock(redisOperation, LOCK_KEY, TimeUnit.HOURS.toSeconds(LOCK_EXPIRED_HOUR))
        if (!redisLock.tryLock()) {
            logger.info("get lock $LOCK_KEY failed, skip cron job")
            return
        }
        val batchCount = 1000
        val expiredTime = LocalDateTime.now().minusDays(7L)
        val lockExpiredTime = LocalDateTime.now().plusHours(LOCK_EXPIRED_HOUR)
        var deletedCount = 0
        try {
            val firstRecord = shortUrlDao.getFirst(dslContext) ?: return
            logger.debug("first short url id: ${firstRecord.id}")
            var count: Int
            var startId = firstRecord.id
            var endId = startId + batchCount
            do {
                val shortUrls = shortUrlDao.getByIdRange(dslContext, startId, endId)
                val deleteIdList = shortUrls.filter { it.expiredTime.isBefore(expiredTime) }.map { it.id }
                if (deleteIdList.isNotEmpty()) {
                    deletedCount += shortUrlDao.delete(dslContext, deleteIdList)
                }
                startId = (shortUrls.lastOrNull()?.id ?: endId) + 1
                endId = startId + batchCount
                count = shortUrls.size
            } while (count > 0 && lockExpiredTime > LocalDateTime.now())
        } catch (e: Exception) {
            logger.error("BKSystemErrorMonitor|cleanShortUrl|error=${e.message}", e)
        } finally {
            redisLock.unlock()
        }
        logger.info("clean $deletedCount short url expired time less than $expiredTime")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShortUrlCleanJob::class.java)
        private const val LOCK_KEY = "cleanExpiredShortUrlJob"
        private const val LOCK_EXPIRED_HOUR = 12L
    }
}
