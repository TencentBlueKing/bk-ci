package com.tencent.devops.experience.job

import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@RefreshScope
class ExperienceRenewJob @Autowired constructor(
    val dslContext: DSLContext,
    val experiencePublicDao: ExperiencePublicDao,
    val experienceDao: ExperienceDao,
    val bkRepoClient: BkRepoClient,
    private val redisOperation: RedisOperation
) {
    @Scheduled(cron = "0 0 1 * * ?")
    @SuppressWarnings("MagicNumber", "NestedBlockDepth", "SwallowedException")
    fun jobRenew() {
        logger.info("experience renew start ... ")

        val redisLock = RedisLock(redisOperation, "expRenewHot", 60L)
        try {
            if (redisLock.tryLock()) {
                val recordIds = experiencePublicDao.listAllRecordId(dslContext)?.map { it.get(0, Long::class.java) }
                ListUtils.partition(recordIds, 100).forEach { rids ->
                    experienceDao.list(dslContext, rids).forEach { record ->
                        if (record.artifactoryType.toUpperCase() == "PIPELINE" && // 流水线构件
                            record.updateTime.plusDays(30).isBefore(LocalDateTime.now()) // 30天前更新
                        ) {
                            try {
                                bkRepoClient.update(
                                    record.creator,
                                    record.projectId,
                                    "pipeline",
                                    record.artifactoryPath,
                                    0
                                )
                            } catch (e: Exception) {
                                logger.warn("update record:${record.id} failed", e)
                            }
                            logger.info("update record:${record.id} success")
                        }
                    }
                }
            } else {
                logger.info("job is running...")
                return
            }
        } catch (e: Exception) {
            logger.error("get records failed !", e)
            return
        }
        logger.info("experience renew finish ... ")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceRenewJob::class.java)
    }
}
