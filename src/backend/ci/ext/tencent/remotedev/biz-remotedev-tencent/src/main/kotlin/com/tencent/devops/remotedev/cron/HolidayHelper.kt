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
        return redisCache.getSetMembers(REDIS_WORKING_ON_WEEKEND_DAY)?.ifEmpty { null } ?: kotlin.run {
            initHolidayInfo()?.first ?: emptySet()
        }
    }

    private fun initHolidayInfo(): Pair<Set<String>, Set<String>>? {
        OkhttpUtils.doGet("https://api.jiejiariapi.com/v1/holidays/2024").use { response ->
            logger.info("initHolidayInfo response|${response.body?.string()}")
            if (!response.isSuccessful) {
                logger.warn("initHolidayInfo fail ,${response.body?.string()}")
                return null
            }
            val res = objectMapper.readValue(response.body?.string(), object : TypeReference<Map<String, Holiday>>() {})
            val workingDays = res.filter { !it.value.offDay }.keys
            val holidays = res.filter { it.value.offDay }.keys

            redisOperation.sadd(REDIS_WORKING_ON_WEEKEND_DAY, *workingDays.toTypedArray())
            redisOperation.sadd(REDIS_HOLIDAY, *holidays.toTypedArray())
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
