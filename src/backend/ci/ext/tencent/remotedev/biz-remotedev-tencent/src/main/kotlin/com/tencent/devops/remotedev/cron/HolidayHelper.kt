package com.tencent.devops.remotedev.cron

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_HOLIDAY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_WORKING_ON_WEEKEND_DAY
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class HolidayHelper @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val redisCache: RedisCacheService
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HolidayHelper::class.java)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * 返回最近几次工作日
     */
    fun getLastWorkingDays(days: Int): List<LocalDateTime> {
        var now = LocalDateTime.now()
        val result = mutableListOf<LocalDateTime>()
        var max = 128
        while (max-- > 0 && result.size < days + 1) {
            if (isWorkingDay(now)) {
                result.add(now)
            }
            now = now.plusDays(-1)
        }
        logger.info(
            "getLastWorkingDays|$days|" +
                    "${LocalDateTime.now().format(formatter)}|${result.last().format(formatter)}"
        )
        return result
    }

    fun isWorkingDay(now: LocalDateTime): Boolean {
        val holidays = getOrInitHolidays(now.year).plus(getOrInitHolidays(now.year - 1))
        val workingDays = getOrInitWorkingDays(now.year).plus(getOrInitWorkingDays(now.year - 1))
        when (now.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> {
                if (now.format(formatter) in workingDays) {
                    return true
                }
            }

            else -> {
                if (now.format(formatter) !in holidays) {
                    return true
                }
            }
        }
        return false
    }

    private fun getOrInitHolidays(year: Int): Set<String> {
        return redisCache.getSetMembers("$REDIS_HOLIDAY:$year")?.ifEmpty { null } ?: kotlin.run {
            initHolidayInfo(year)?.second ?: emptySet()
        }
    }

    private fun getOrInitWorkingDays(year: Int): Set<String> {
        return redisCache.getSetMembers("$REDIS_WORKING_ON_WEEKEND_DAY:$year")?.ifEmpty { null } ?: kotlin.run {
            initHolidayInfo(year)?.first ?: emptySet()
        }
    }

    private fun initHolidayInfo(year: Int): Pair<Set<String>, Set<String>>? {
        OkhttpUtils.doGet("https://api.jiejiariapi.com/v1/holidays/$year").use { response ->
            val body = response.body?.string()
            logger.info("initHolidayInfo response|$body")
            if (!response.isSuccessful) {
                logger.warn("initHolidayInfo fail ,$body")
                return null
            }
            val res = objectMapper.readValue(body, object : TypeReference<Map<String, Holiday>>() {})
            val workingDays = res.filter { !it.value.offDay }.keys
            val holidays = res.filter { it.value.offDay }.keys

            redisOperation.sadd("$REDIS_WORKING_ON_WEEKEND_DAY:$year", *workingDays.toTypedArray())
            redisOperation.sadd("$REDIS_HOLIDAY:$year", *holidays.toTypedArray())
            return Pair(workingDays, holidays)
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Holiday(
        @JsonProperty("date")
        val date: String,
        @JsonProperty("name")
        val name: String,
        @JsonProperty("isOffDay")
        val offDay: Boolean
    )
}
