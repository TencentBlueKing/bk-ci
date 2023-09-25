package com.tencent.devops.common.service.health

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health

import org.springframework.boot.actuate.health.HealthIndicator

/**
 * 内存容量检查, 小于100MB时健康检查失败
 */
class MemoryHealthIndicator : HealthIndicator {
    override fun health(): Health {
        val freeMemory = Runtime.getRuntime().freeMemory()
        return if (freeMemory < THRESHOLD) {
            logger.error("Not enough free memory , freeMemory: $freeMemory")
            Health.down().withDetail("error", "Not enough free memory").build()
        } else {
            Health.up().build()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MemoryHealthIndicator::class.java)
        private const val THRESHOLD = 100 * 1024 * 1024 // 100MB
    }
}
