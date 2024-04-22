package com.tencent.devops.environment.cron

import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentCronClean @Autowired constructor(
    private val dslContext: DSLContext,
    private val actionDao: ThirdPartyAgentActionDao
) {
    // 清理超过100的Action数据
    @Scheduled(cron = "0 28 3 * * ?")
    fun cronDeleteActionRecord() {
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
    }
}
