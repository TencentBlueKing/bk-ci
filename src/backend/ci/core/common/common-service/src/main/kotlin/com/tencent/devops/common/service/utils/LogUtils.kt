package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.util.Watcher
import org.slf4j.LoggerFactory

object LogUtils {
    private val logger = LoggerFactory.getLogger(LogUtils::class.java)

    fun costTime(message: String, startTime: Long) {
        val endTime = System.currentTimeMillis()
        val cost = endTime - startTime
        when (cost) {
            in 0..999 -> {
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

    /**
     * 计算[watcher].createTime与当前时间的毫秒数的耗时在[warnThreshold]与[errorThreshold]之间，
     * 会将[watcher]序列化为字符串并打印到WARN日志，当超出[errorThreshold]会打印ERROR日志。否则什么都不会打印
     */
    fun printCostTimeWE(watcher: Watcher, warnThreshold: Long = 1000, errorThreshold: Long = 5000) {
        watcher.stop()
        val endTime = System.currentTimeMillis()
        val cost = endTime - watcher.createTime
        if (cost >= warnThreshold) {
            if (cost > errorThreshold) {
                logger.error("$watcher cost $cost ms")
            } else {
                logger.warn("$watcher cost $cost ms")
            }
        }
    }
}
