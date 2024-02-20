package com.tencent.devops.remotedev.cron

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_HOLIDAY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_WORKING_ON_WEEKEND_DAY
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        val logger = LoggerFactory.getLogger(HolidayHelper::class.java)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")
    }

    /**
     * 返回最近几次工作日
     */
    fun getLastWorkingDays(days: Int): List<LocalDateTime> {
        var now = LocalDateTime.now()
        val result = mutableListOf<LocalDateTime>()
        val holidays = getOrInitHolidays()
        val workingDays = getOrInitWorkingDays()
        var max = 32
        while (max-- > 0 && result.size < days) {
            when (now.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> {
                    if (now.format(formatter) in workingDays) {
                        result.add(now)
                    }
                }

                else -> {
                    if (now.format(formatter) !in holidays) {
                        result.add(now)
                    }
                }
            }
            now = now.plusDays(-1)
        }
        logger.info(
            "getLastWorkingDays|$days|" +
                    "${LocalDateTime.now().format(formatter)}|${result.last().format(formatter)}"
        )
        return result
    }

    private fun getOrInitHolidays(): Set<String> {
        return redisCache.getSetMembers(REDIS_HOLIDAY)?.ifEmpty { null } ?: kotlin.run {
            initHolidayInfo()?.second ?: emptySet()
        }
    }

    private fun getOrInitWorkingDays(): Set<String> {
        return redisCache.getSetMembers(REDIS_WORKING_ON_WEEKEND_DAY)?.ifEmpty { null }  ?: kotlin.run {
            initHolidayInfo()?.first ?: emptySet()
        }
    }

    private fun initHolidayInfo(): Pair<Set<String>, Set<String>>? {
        OkhttpUtils.doGet("https://timor.tech/api/holiday/year").use { response ->
            logger.info("initHolidayInfo response|${response.body.toString()}")
            if (!response.isSuccessful) {
                logger.warn("initHolidayInfo fail ,${response.body}")
                return null
            }
            val res = objectMapper.readValue(response.body.toString(), HolidayInfo::class.java)
            val workingDays = res.holiday.filter { !it.value.holiday }.keys
            val holidays = res.holiday.filter { it.value.holiday }.keys

            redisOperation.sadd(REDIS_WORKING_ON_WEEKEND_DAY, *workingDays.toTypedArray())
            redisOperation.sadd(REDIS_HOLIDAY, *holidays.toTypedArray())
            return Pair(workingDays, holidays)
        }
    }

    data class HolidayInfo(
        @JsonProperty("code")
        val code: Int,
        @JsonProperty("holiday")
        val holiday: Map<String, Holiday>
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Holiday(
        @JsonProperty("date")
        val date: String,
        @JsonProperty("holiday")
        val holiday: Boolean
    )
}
