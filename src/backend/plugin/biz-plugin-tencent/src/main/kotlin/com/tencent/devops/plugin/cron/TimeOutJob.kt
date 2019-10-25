package com.tencent.devops.plugin.cron

import com.tencent.devops.plugin.dao.JinGangAppDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TimeOutJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val jinGangAppDao: JinGangAppDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TimeOutJob::class.java)
    }

    // 一个小时
    @Scheduled(initialDelay = 5000, fixedDelay = 60*60*1000)
    fun timeOutJob() {
        logger.info("<<< timeOutJob >>>")
        jinGangAppDao.timeOutJob(dslContext)
    }
}