package com.tencent.devops.experience.job

import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@RefreshScope
class ExperienceRenewJob @Autowired constructor(
    val dslContext: DSLContext,
    val experiencePublicDao: ExperiencePublicDao,
    val experienceDao: ExperienceDao,
    val bkRepoClient: BkRepoClient
) {
    @Scheduled(cron = "0 * * * * ?") // TODO 改成一天一次
    @SuppressWarnings("MagicNumber", "NestedBlockDepth", "SwallowedException")
    fun jobRenew() {
        logger.info("experience renew start ... ")

        val updateList = mutableListOf<Pair<String/*projectId*/, String/*path*/>>()

        try {
            val recordIds = experiencePublicDao.listAllRecordId(dslContext)?.map { it.get(0, Long::class.java) }
            ListUtils.partition(recordIds, 100).forEach { rids ->
                experienceDao.list(dslContext, rids).forEach { record ->
                    if (record.artifactoryType.toUpperCase() == "PIPELINE") {
                        updateList.add(Pair(record.projectId, record.artifactoryPath))
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("get records failed !", e)
        }

        for (updatePair in updateList) {
            try {
                bkRepoClient.update(
                    "admin",
                    updatePair.first,
                    "pipeline",
                    updatePair.second,
                    0
                )
            } catch (e: Exception) {
                logger.error("update pair:$updatePair failed", e)
            }
            logger.info("update pair:$updatePair success")
        }

        logger.info("experience renew finish ... ")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceRenewJob::class.java)
    }
}
