package com.tencent.devops.process.util

import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

/**
 * @author: carrypan
 * @date: 2019/03/06
 */
object DateTimeUtils {

    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    private val logger = LoggerFactory.getLogger(DateTimeUtils::class.java)

    /**
     * 单位转换，分钟转换秒
     */
    fun minuteToSecond(minutes: Int): Int {
        return minutes * 60
    }

    /**
     * 单位转化，秒转换分钟
     * 以Int为计算单位，有余数将省去
     */
    fun secondToMinute(seconds: Int): Int {
        return seconds / 60
    }

    /**
     * 单位转化，秒转换时间戳
     * 2019-09-02T08:58:46+0000 -> xxxxx
     */
    fun zoneDateToTimestamp(timeStr: String?): Long {
        try {
            if (timeStr.isNullOrBlank()) return 0L
            return formatter.parse(timeStr).time
        } catch (e: Exception) {
            logger.error("fail to parse time string: $timeStr", e)
        }
        return 0L
    }
}