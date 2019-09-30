package com.tencent.devops.log.cron

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.v2.IndexDaoV2
import com.tencent.devops.log.dao.v2.LogStatusDaoV2
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 清理`T_LOG_INDICES_V2` `T_LOG_STATUS_V2`三个月前的构建
 */
@Component
class CleanBuildJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val indexDaoV2: IndexDaoV2,
    private val logStatusDaoV2: LogStatusDaoV2
) {

    private var expireBuildInDay = 30 * 6 // 半年

    @Scheduled(cron = "0 0 3 * * ?")
    fun cleanBuilds() {
        logger.info("Start to clean builds")
        val redisLock = RedisLock(redisOperation, CLEAN_BUILD_JOB_REDIS_KEY, 20)
        try {
            val lockSuccess = redisLock.tryLock()
            if (!lockSuccess) {
                logger.info("The other process is processing clean job")
                return
            }
            clean()
            logger.info("Finish cleaning the builds")
        } catch (t: Throwable) {
            logger.warn("Fail to clean builds", t)
        } finally {
            redisLock.unlock()
        }
    }

    fun expire(expired: Int) {
        logger.info("Update the expired from $expireBuildInDay to $expired")
        if (expired <= 30) {
            logger.warn("The expired is illegal")
            throw OperationException("The expired param is illegal")
        }
        expireBuildInDay = expired
    }

    fun getExpire() = expireBuildInDay

    private fun clean() {
        logger.info("Cleaning the builds")
        while (true) {
            val records = indexDaoV2.listLatestBuilds(dslContext, 10)
            if (records.isEmpty()) {
                logger.info("The record is empty")
                return
            }

            val buildIds = records.filter {
                expire(it.createdTime.timestamp())
            }.map { it.buildId }.toSet()

            if (buildIds.isEmpty()) {
                logger.info("Done cleaning the builds")
                return
            }
            logger.info("The builds[$buildIds] need to be cleaned")
            cleanInDB(buildIds)
        }
    }

    private fun cleanInDB(buildIds: Set<String>) {
        if (buildIds.isEmpty()) {
            return
        }
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val indexDaoCnt = indexDaoV2.delete(context, buildIds)
            val statusCnt = logStatusDaoV2.delete(context, buildIds)
            logger.info("[$indexDaoCnt|$statusCnt] Delete the builds")
        }
    }

    private fun expire(timestamp: Long): Boolean {
        return (System.currentTimeMillis() / 1000 - timestamp) >= TimeUnit.DAYS.toSeconds(expireBuildInDay.toLong()) // expire in 90 days
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanBuildJob::class.java)
        private const val CLEAN_BUILD_JOB_REDIS_KEY = "log:clean:build:job:lock:key"
    }
}