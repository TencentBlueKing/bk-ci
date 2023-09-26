package com.tencent.devops.common.service.health

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health

/**
 * 内存容量检查, 小于100MB时健康检查失败
 */
class MemoryHealthIndicator : AbstractHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder) {
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
        private val logger = LoggerFactory.getLogger(MemoryHealthIndicator::class.java)
        private const val THRESHOLD = 100 * 1024 * 1024 // 100MB
    }
}
