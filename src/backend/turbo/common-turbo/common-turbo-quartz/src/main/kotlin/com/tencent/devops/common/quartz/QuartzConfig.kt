package com.tencent.devops.common.quartz

import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_GENERAL_SYSTEM_FAIL
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.FileSystemResource
import java.io.File

@Suppress("MaxLineLength")
@Configuration
class QuartzConfig {

    companion object {
        private val logger = LoggerFactory.getLogger(QuartzConfig::class.java)
    }

    @Bean
    fun schedulerCustomizer(environment: Environment): SchedulerFactoryBeanCustomizer {
        val thirdPartyPath = environment.getProperty("turbo.thirdparty.propdir", "")
        val quartzPropertyFile = File("$thirdPartyPath/quartz.properties")
        if (!quartzPropertyFile.exists()) {
            logger.info("quartz property file not exists")
            throw TurboException(errorCode = TURBO_GENERAL_SYSTEM_FAIL, errorMessage = "quartz property file not exists")
        }
        return SchedulerFactoryBeanCustomizer {
            it.setConfigLocation(FileSystemResource("$thirdPartyPath/quartz.properties"))
        }
    }
}
