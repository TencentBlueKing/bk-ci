package com.tencent.devops.project.service.job

import com.tencent.devops.project.service.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * deng
 * 2019-01-14
 */
@Service
class SyncCCAppNameJobService @Autowired constructor(private val synProjectService: SynProjectService) {

    companion object {
        private val logger = LoggerFactory.getLogger(SyncCCAppNameJobService::class.java)
    }

    @Scheduled(cron = "0 0 4 * * ?") // 每天早上4点执行一次
    fun syncCCName() {
        logger.info("Start to sync project cc name")
        val count = synProjectService.syncCCAppName()
        logger.info("Success to sync $count cc names")
    }
}