package com.tencent.devops.common.util

import org.slf4j.LoggerFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * 日期工具类
 */
object DateTimeUtils {
    private val logger = LoggerFactory.getLogger(DateTimeUtils::class.java)

    private const val YYYY_MM_DD_FORMAT = "yyyy-MM-dd"

    /**
     * 获取当前日期前X天的日期
     *
     * @return
     */
    fun getBeforeDaily(calendarStr: Int, day: Int): String {
        val format = SimpleDateFormat("yyyy-MM-dd")
        val c = Calendar.getInstance()
        c.time = Date()
        c.add(calendarStr, -day)
        return format.format(c.time)
    }

    /**
     * 根据日期字符串获取时间戳
     *
     * @param startCreateDate
     * @param endCreateDate
     * @return
     */
    fun getStartTimeAndEndTime(startCreateDate: String?, endCreateDate: String?): LongArray? {
        var startTime: Long = 0
        var endTime: Long = 0
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        try {
            startTime = df.parse("$startCreateDate 00:00:00").time
            endTime = df.parse("$endCreateDate 23:59:59").time
        } catch (e: Exception) {
            logger.info("输入的开始时间或结束时间有误！ 开始时间：$startCreateDate，结束时间：$endCreateDate")
        }
        return longArrayOf(startTime, endTime)
    }

    /**
     * 获取开始日期和结束日期中间所有的时间集合
     */
    fun getStartTimeBetweenEndTime(startTime: String, endTime: String): MutableList<String> {
        val sdf = SimpleDateFormat(YYYY_MM_DD_FORMAT)
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(startTime)
        val date = mutableListOf<String>()

        var d = calendar.timeInMillis
        while (d <= sdf.parse(endTime).time) {
            date.add(sdf.format(d))
            d = getPlusDayMillis(calendar)
        }
        return date
    }

    private fun getPlusDayMillis(c: Calendar): Long {
        c[Calendar.DAY_OF_MONTH] = c[Calendar.DAY_OF_MONTH] + 1
        return c.timeInMillis
    }

    /**
     * 从一个具体时间，比如2016-12-12 23:23:15，获得秒数
     *
     * @param time
     * @return
     */
    fun getTimeStamp(time: String?): Long {
        if (time == null || time.isEmpty()) {
            return 0
        }
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val date = try {
            simpleDateFormat.parse(time)
        } catch (e: ParseException) {
            logger.info("parse string time[$time] to timestamp failed")
            return 0
        }
        return date.time
    }

    /**
     * 将LocalDate转为时间字符串
     */
    fun localDateTransformTimestamp(localDate: LocalDateTime?): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(localDate)
    }

    /**
     * 将LocalDate转为日期字符串
     */
    fun localDate2DateStr(localDate: LocalDate?): String {
        return DateTimeFormatter.ofPattern(YYYY_MM_DD_FORMAT).format(localDate)
    }

    /**
     * 将日期字符串转为LocalDate
     */
    fun dateStr2LocalDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(YYYY_MM_DD_FORMAT))
    }
}
