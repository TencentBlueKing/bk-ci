package com.tencent.devops.log.cron

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.util.IndexNameUtils.LOG_PREFIX
import org.elasticsearch.client.transport.TransportClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Component
class ESIndexCloseJob @Autowired constructor(
    private val client: TransportClient,
    private val redisOperation: RedisOperation
) {

    private var expireIndexInDay = 30 // default is expire in 30 days

    /**
     * 2 am every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun closeIndex() {
        logger.info("Start to close index")
        val redisLock = RedisLock(redisOperation, ES_INDEX_CLOSE_JOB_KEY, 20)
        try {
            if (!redisLock.tryLock()) {
                logger.info("The other process is processing clean job, ignore")
                return
            }
            closeESIndexes()
        } catch (t: Throwable) {
            logger.warn("Fail to close the index", t)
        } finally {
            redisLock.unlock()
        }
    }

    fun updateExpireIndexDay(expired: Int) {
        logger.warn("Update the expire index day from $expired to ${this.expireIndexInDay}")
        if (expired <= 10) {
            logger.warn("The expired is illegal")
            throw OperationException("Expired is illegal")
        }
        this.expireIndexInDay = expired
    }

    fun getExpireIndexDay() = expireIndexInDay

    private fun closeESIndexes() {
        val indexes = client.admin()
            .indices()
            .prepareGetIndex()
            .get()

        if (indexes.indices.isEmpty()) {
            return
        }

        val deathLine = LocalDateTime.now()
                .minus(expireIndexInDay.toLong(), ChronoUnit.DAYS)
        logger.info("Get the death line - ($deathLine)")
        indexes.indices.forEach { index ->
            if (expire(deathLine, index)) {
                closeESIndex(index)
            }
        }
    }

    private fun closeESIndex(index: String) {
        logger.info("[$index] Start to close ES index")
        val resp = client.admin()
            .indices()
            .prepareClose(index)
            .get()
        logger.info("Get the close es response - ${resp.isAcknowledged}")
    }

    private fun expire(deathLine: LocalDateTime, index: String): Boolean {
        try {
            if (!index.startsWith(LOG_PREFIX)) {
                return false
            }
            val dateStr = index.replace(LOG_PREFIX, "") + " 00:00"
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = LocalDateTime.parse(dateStr, format)

            if (deathLine > date) {
                logger.info("[$index] The index is expire ($deathLine|$date)")
                return true
            }
        } catch (t: Throwable) {
            logger.warn("[$index] Fail to check if the index expire", t)
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ESIndexCloseJob::class.java)
        private const val ES_INDEX_CLOSE_JOB_KEY = "log:es:index:close:job:lock:key"
    }
}