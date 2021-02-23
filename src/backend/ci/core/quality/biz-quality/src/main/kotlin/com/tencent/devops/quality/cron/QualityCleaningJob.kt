package com.tencent.devops.quality.cron

import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.dao.v2.QualityHisMetadataDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@EnableScheduling
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class QualityCleaningJob @Autowired constructor(
    private val qualityHisMetadataDao: QualityHisMetadataDao,
    private val historyDao: HistoryDao,
    private val dslContext: DSLContext
){

    private val logger = LoggerFactory.getLogger(QualityCleaningJob::class.java)

    @Value("\${quality.clean.meta.times:1200}")
    private val cleanMetaTimes = 1200

    @Value("\${quality.clean.meta.pageSize:1000}")
    private val cleanMetaPageSize = 1000

    @Scheduled(cron = "0 0 2 * * ?")
    fun cleanMetaDetail() {
        logger.info("start to clean meta data: $cleanMetaTimes, $cleanMetaPageSize")
        var count = 0;

        for (i in 1..cleanMetaTimes) {
            val buildIdSet = qualityHisMetadataDao.getHisOriginMetadataBuildId(dslContext, cleanMetaPageSize).map { it.value1() }.toSet()

            if (buildIdSet.size < cleanMetaPageSize) {
                logger.info("too small meta data size and exit: ${buildIdSet.size}")
                break
            }

            val finishBuildIdSet = historyDao.listByBuildId(dslContext, buildIdSet).map { it.buildId }.toSet()
            count += finishBuildIdSet.size

            if (finishBuildIdSet.isNotEmpty()) {
                logger.info("start to delete quality meta data: ${finishBuildIdSet.size}, ${finishBuildIdSet.first()}")
                qualityHisMetadataDao.deleteHisDetailMetadataByBuildId(dslContext, finishBuildIdSet)
                qualityHisMetadataDao.deleteHisOriginMetadataByBuildId(dslContext, finishBuildIdSet)
            }

            Thread.sleep(6000)
        }

        logger.info("finish to clean meta data build size: $count")
    }
}