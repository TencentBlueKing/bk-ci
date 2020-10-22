package com.tencent.devops.common.service.utils

import org.slf4j.LoggerFactory

object LogUtils {
    private val logger = LoggerFactory.getLogger(LogUtils::class.java)

    fun costTime(message: String, startTime: Long) {
        val endTime = System.currentTimeMillis()
        val cost = endTime - startTime
        when (cost) {
            in 1..999 -> {
                logger.info("$message cost $cost ms")
            }
            in 1000..5000 -> {
                logger.warn("$message cost $cost ms")
            }
            else -> {
                logger.error("$message cost $cost ms")
            }
        }
    }
}