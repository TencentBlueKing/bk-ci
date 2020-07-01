package com.tencent.devops.project.utils

import org.slf4j.Logger

object CostUtils {
    fun costTime(startTime: Long, url: String, logger: Logger) {
        val endTime = System.currentTimeMillis()
        val cost = endTime - startTime
        val log = "call tof $url cost $cost"
        when {
            cost < 1000 -> {
                logger.info(log)
            }
            cost in 1001..9999 -> {
                logger.warn(log)
            }
            cost > 10000 -> {
                logger.error(log)
            }
        }
    }
}