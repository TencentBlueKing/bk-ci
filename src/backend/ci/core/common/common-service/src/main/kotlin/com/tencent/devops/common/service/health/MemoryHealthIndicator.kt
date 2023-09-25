package com.tencent.devops.common.service.health

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.availability.ApplicationAvailability

/**
 * 自定义健康检查
 */
class CustomLivenessStateHealthIndicator(applicationAvailability: ApplicationAvailability) :
    LivenessStateHealthIndicator(applicationAvailability) {
    override fun doHealthCheck(builder: Health.Builder) {
        super.doHealthCheck(builder)
        // 内存容量检查, 小于100MB时健康检查失败
        val freeMemory = Runtime.getRuntime().freeMemory()
        if (freeMemory >= THRESHOLD) {
            builder.up().withDetail("freeMemory", freeMemory)
        } else {
            val errorMsg = "Not enough free memory , freeMemory: $freeMemory"
            logger.error(errorMsg)
            throw RuntimeException(errorMsg)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CustomLivenessStateHealthIndicator::class.java)
        private const val THRESHOLD = 100 * 1024 * 1024 // 100MB
    }

}
