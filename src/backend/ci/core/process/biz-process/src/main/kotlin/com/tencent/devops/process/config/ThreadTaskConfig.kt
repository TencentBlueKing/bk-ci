package com.tencent.devops.process.config

import com.tencent.devops.process.service.PipelineClearService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

/**
 * @Description
 * @Date 2020/3/4
 * @Version 1.0
 */
@Configuration
class ThreadTaskConfig @Autowired constructor(
    private val pipelineClearService: PipelineClearService
) {

    private val logger = LoggerFactory.getLogger(ThreadTaskConfig::class.java)

    @EventListener
    fun event(event: ApplicationReadyEvent) {
        logger.info("Application Ready")
        pipelineClearService.clear()
    }
}
