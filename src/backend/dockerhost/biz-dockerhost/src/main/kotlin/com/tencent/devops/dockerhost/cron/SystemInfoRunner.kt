package com.tencent.devops.dockerhost.cron

import com.tencent.devops.dockerhost.utils.SigarUtil
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SystemInfoRunner {
    private val logger = LoggerFactory.getLogger(SystemInfoRunner::class.java)

    @Scheduled(cron = "0/10 * * * * ?")
    fun startCollect() {
        try {
            SigarUtil.pushMem()
            SigarUtil.pushCpu()
        } catch (t: Throwable) {
            logger.error("Start collect system info unknown exception", t)
        }
    }
}