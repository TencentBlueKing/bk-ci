package com.tencent.devops.experience.job

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceDownloadDetailDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@RefreshScope
@SuppressWarnings("TooGenericExceptionCaught", "MagicNumber")
class ExperienceHotJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceDao: ExperienceDao,
    private val experienceDownloadDetailDao: ExperienceDownloadDetailDao,
    private val dslContext: DSLContext
) {
    @Value("\${recommend.hot.days:7}")
    private var hotDays: Long = 7

    @Scheduled(cron = "0 5 0 * * ?")
    @SuppressWarnings("MagicNumber", "NestedBlockDepth", "SwallowedException")
    fun jobHot() {
        if (hotDays <= 0) {
            logger.info("some params is null , jobHot no start")
            return
        }
        val redisLock = RedisLock(redisOperation, "expJobHot", 60L)
        try {
            logger.info("Job hot start")
            if (redisLock.tryLock()) {
                val hotDaysAgo = LocalDateTime.now().minusDays(hotDays)
                val publicUnique = experiencePublicDao.listAllUnique(dslContext)
                publicUnique.forEach {
                    val countForHot = experienceDownloadDetailDao.countForHot(
                        dslContext = dslContext,
                        projectId = it.projectId,
                        bundleIdentifier = it.bundleIdentifier,
                        platform = it.platform,
                        hotDaysAgo = hotDaysAgo
                    )

                    val updateTime = try {
                        experienceDao.get(dslContext, it.get("RECORD_ID", Long::class.java)).updateTime
                    } catch (e: Exception) {
                        it.createTime
                    }

                    experiencePublicDao.updateById(
                        dslContext = dslContext,
                        id = it.get("ID", Long::class.java),
                        downloadTime = countForHot,
                        updateTime = updateTime
                    )
                }
                logger.info("Job hot finish")
            } else {
                logger.info("Job hot is running")
            }
        } catch (e: Throwable) {
            logger.error("Job hot error:", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceHotJob::class.java)
    }
}
