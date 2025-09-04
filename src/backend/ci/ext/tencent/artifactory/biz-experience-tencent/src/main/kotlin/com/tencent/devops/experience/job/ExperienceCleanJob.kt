package com.tencent.devops.experience.job

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.pojo.ExperienceClean
import com.tencent.devops.experience.service.ExperienceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExperienceCleanJob @Autowired constructor(
    private val experienceDao: ExperienceDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val experienceService: ExperienceService
) {
    @Scheduled(cron = "0 10 0 * * ?")
    fun clean() {
        val redisLock = RedisLock(redisOperation, "expJobClean", 60L)
        try {
            logger.info("Experience clean job start")
            if (redisLock.tryLock()) {
                val cleanIds = experienceDao.listCleanIds(dslContext)
                logger.info("Experience clean job doing , clean ids : $cleanIds")
                experienceService.clean(ExperienceClean(cleanIds))
                logger.info("Experience clean job success")
            } else {
                logger.warn("Experience clean job stop for redis lock")
            }
        } catch (e: Exception) {
            logger.error("Experience clean job failed", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceHotJob::class.java)
    }
}