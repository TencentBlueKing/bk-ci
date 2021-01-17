package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.util.Watcher
import org.slf4j.LoggerFactory

object LogUtils {

    private val LOG = LoggerFactory.getLogger(LogUtils::class.java)

    fun costTime(message: String, startTime: Long, warnThreshold: Long = 1000, errorThreshold: Long = 5000) {
        val cost = System.currentTimeMillis() - startTime
        when {
            cost < warnThreshold -> {
                LOG.info("$message cost $cost ms")
            }
            cost in warnThreshold until errorThreshold -> {
                LOG.warn("$message cost $cost ms")
            }
            else -> {
                LOG.error("$message cost $cost ms")
            }
        }
    }

    /**
     * 计算[watcher].createTime与当前时间的毫秒数的耗时在[warnThreshold]与[errorThreshold]之间，
     * 会将[watcher]序列化为字符串并打印到WARN日志，当超出[errorThreshold]会打印ERROR日志。否则什么都不会打印
     */
    fun printCostTimeWE(watcher: Watcher, warnThreshold: Long = 1000, errorThreshold: Long = 5000) {
        watcher.stop()
        val cost = System.currentTimeMillis() - watcher.createTime
        if (cost >= warnThreshold) {
            if (cost > errorThreshold) {
                LOG.error("$watcher cost $cost ms")
            } else {
                LOG.warn("$watcher cost $cost ms")
            }
        }
    }
}
