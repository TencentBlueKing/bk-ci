package com.tencent.devops.dispatch.cron

import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * deng
 * 2019-06-11
 * 周期性清理第三方构建机任务的状态
 */
@Component
class ThirdPartyAgentCleanupJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {

    // every 30 minutes
    @Scheduled(initialDelay = 10 * 60 * 1000, fixedDelay = 30 * 60 * 1000)
    fun cleanup() {
        logger.info("Start to clean up the third party agent")
        try {
            val expiredBuilds = thirdPartyAgentBuildDao.getExpireBuilds(dslContext)
            if (expiredBuilds.isEmpty()) {
                logger.info("Expire build is empty")
                return
            }
            val ids = expiredBuilds.map { it.id }.toSet()
            logger.info("Get the expire builds - [$expiredBuilds] - [$ids]")
            val count = thirdPartyAgentBuildDao.updateExpireBuilds(dslContext, ids)
            logger.info("Update $count expired agent builds")
        } catch (t: Throwable) {
            logger.warn("Fail to clean up the third party agent")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentCleanupJob::class.java)
    }
}