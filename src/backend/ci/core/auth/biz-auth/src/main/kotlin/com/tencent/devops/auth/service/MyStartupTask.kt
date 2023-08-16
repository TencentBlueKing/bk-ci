package com.tencent.devops.auth.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class MyStartupTask  {
    @PostConstruct
    fun init() {
        logger.info("MyStartupTask init")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MyStartupTask::class.java)
    }
}
