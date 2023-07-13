/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.FAIL_NUM
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.StoreErrorCodeInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.common.KEY_HOT_FLAG
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreStatistic
import com.tencent.devops.store.pojo.common.StoreStatisticPipelineNumUpdate
import com.tencent.devops.store.pojo.common.StoreStatisticTrendData
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreDailyStatisticService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Record7
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StoreTotalStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeErrorCodeInfoDao: StoreErrorCodeInfoDao,
    private val storeDailyStatisticService: StoreDailyStatisticService
) : StoreTotalStatisticService {

    @Value("\${statistics.timeSpanMonth:-3}")
    private val timeSpanMonth: Int = -3

    companion object {
        private val logger = LoggerFactory.getLogger(StoreTotalStatisticServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 50
        private const val DEFAULT_PERCENTILE = 0.8
    }

    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    fun stat() {
        val lock = RedisLock(redisOperation, "storeTotalStatistic", 60000L)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            val taskName = "StoreTotalStatisticTask"
            logger.info("$taskName:stat:start")
            StoreTypeEnum.values().forEach { storeType ->
                val type = storeType.type.toByte()
                val percentileValue = percentileCalculation(storeType, DEFAULT_PERCENTILE)
                logger.info("StoreTotalStatisticTask getStorePercentileValue $percentileValue")
                // 类型组件百分位数计算正常，放入缓存待用
                if (percentileValue > 0.0) {
                    redisOperation.set(
                        "STORE_${storeType.name}_PERCENTILE_VALUE",
                        "$percentileValue",
                        TimeUnit.DAYS.toSeconds(1L)
                    )
                }
                var offset = 0
                do {
                    val storeCodes = storeStatisticTotalDao.batchGetStatisticByStoreCode(
                        dslContext = dslContext,
                        storeType = type,
                        offset = offset,
                        limit = DEFAULT_PAGE_SIZE
                    )
                    val statistics = storeStatisticDao.batchGetStatisticByStoreCode(
                        dslContext = dslContext,
                        storeCodeList = storeCodes,
                        storeType = type
                    )
                    calculateAndStorage(
                        storeType = type,
                        statistics = statistics,
                        storeCodes = storeCodes
                    )
                    offset += DEFAULT_PAGE_SIZE
                } while (storeCodes.size == DEFAULT_PAGE_SIZE)
            }
            logger.info("$taskName:stat:end")
        } catch (ignored: Throwable) {
            logger.warn("storeTotalStatistic failed", ignored)
        } finally {
            lock.unlock()
        }
    }
    private fun calculateAndStorage(
        storeType: Byte,
        storeCodes: List<String>,
        statistics: Result<Record4<BigDecimal, BigDecimal, BigDecimal, String>>
    ) {
        storeCodes.forEach { storeCode ->
            val statistic = statistics.firstOrNull { it.value4().toString() == storeCode }
            if (statistic != null) {
                // 下载量
                val downloads = statistic.value1().toInt()
                // 评论数量
                val comments = statistic.value2().toInt()
                // 评论总分
                val score = statistic.value3().toDouble()
                calculateAndStorage(
                    storeType = storeType,
                    downloads = downloads,
                    comments = comments,
                    score = score,
                    storeCode = storeCode
                )
            } else {
                calculateAndStorage(
                    storeType = storeType,
                    storeCode = storeCode
                )
            }
        }
    }

    override fun updateStoreTotalStatisticByCode(storeCode: String, storeType: Byte) {
        val statistics = storeStatisticDao.batchGetStatisticByStoreCode(
            dslContext = dslContext,
            storeCodeList = listOf(storeCode),
            storeType = storeType
        )
        statistics.forEach {
            // 下载量
            val downloads = it.value1().toInt()
            // 评论数量
            val comments = it.value2().toInt()
            // 评论总分
            val score = it.value3().toDouble()
            val code = it.value4().toString()
            calculateAndStorage(
                storeType = storeType,
                downloads = downloads,
                comments = comments,
                score = score,
                storeCode = code
            )
        }
    }

    override fun getStatisticByCode(
        userId: String,
        storeType: Byte,
        storeCode: String
    ): StoreStatistic {
        val record =
            storeStatisticTotalDao.getStatisticByStoreCode(dslContext, storeCode, storeType)
        // 统计基于昨天为截止日期的最近三个月的数据的组件执行成功率
        val endTime = DateTimeUtil.convertDateToFormatLocalDateTime(
            date = DateTimeUtil.getFutureDateFromNow(Calendar.DAY_OF_MONTH, -1),
            format = "yyyy-MM-dd"
        )
        val dailyStatisticList = storeDailyStatisticService.getDailyStatisticListByCode(
            storeCode = storeCode,
            storeType = storeType,
            startTime = DateTimeUtil.convertDateToLocalDateTime(
                DateTimeUtil.getFutureDate(
                    localDateTime = endTime,
                    unit = Calendar.MONTH,
                    timeSpan = timeSpanMonth
                )
            ),
            endTime = endTime
        )
        var successRate: Double? = null
        if (dailyStatisticList != null) {
            var totalSuccessNum = 0
            var totalFailNum = 0
            dailyStatisticList.forEach { dailyStatistic ->
                totalSuccessNum += dailyStatistic.dailySuccessNum
                totalFailNum += dailyStatistic.dailyFailNum
            }
            val totalNum = totalSuccessNum + totalFailNum
            successRate =
                if (totalNum > 0) String.format("%.2f", totalSuccessNum.toDouble() * 100 / totalNum)
                    .toDouble() else null
        }
        return generateStoreStatistic(record, successRate)
    }

    override fun getStatisticByCodeList(
        storeType: Byte,
        storeCodeList: List<String>
    ): HashMap<String, StoreStatistic> {
        val records = storeStatisticTotalDao.batchGetStatisticByStoreCode(
            dslContext = dslContext,
            storeCodeList = storeCodeList,
            storeType = storeType
        )
        val storeStatisticMap = hashMapOf<String, StoreStatistic>()
        records?.map {
            val storeCode = it.get(KEY_STORE_CODE) as? String
            if (storeCode != null) {
                storeStatisticMap[storeCode] = generateStoreStatistic(it)
            }
        }
        return storeStatisticMap
    }

    override fun updatePipelineNum(pipelineNumUpdateList: List<StoreStatisticPipelineNumUpdate>, storeType: Byte) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeStatisticTotalDao.batchUpdatePipelineNum(
                dslContext = context,
                pipelineNumUpdateList = pipelineNumUpdateList,
                storeType = storeType
            )
        }
    }

    override fun getStatisticTrendDataByCode(
        userId: String,
        storeType: Byte,
        storeCode: String,
        startTime: String,
        endTime: String
    ): StoreStatisticTrendData {
        logger.info("getStatisticTrendDataByCode params:[$userId,$storeCode,$storeType,$startTime,$endTime]")
        // 判断当前用户是否是该组件的成员
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType
            )
        ) {
            throw ErrorCodeException(
                errorCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        val dailyStatisticList = storeDailyStatisticService.getDailyStatisticListByCode(
            storeCode = storeCode,
            storeType = storeType,
            startTime = DateTimeUtil.stringToLocalDateTime(startTime),
            endTime = DateTimeUtil.stringToLocalDateTime(endTime)
        )
        var totalFailNum = 0
        var totalSystemFailNum = 0
        var totalUserFailNum = 0
        var totalThirdFailNum = 0
        var totalComponentFailNum = 0
        dailyStatisticList?.forEach { dailyStatistic ->
            totalFailNum += dailyStatistic.dailyFailNum
            val dailyFailDetail = dailyStatistic.dailyFailDetail
            if (dailyFailDetail != null) {
                totalSystemFailNum += dailyFailDetail["totalSystemFailNum"] as? Int ?: 0
                totalUserFailNum += dailyFailDetail["totalUserFailNum"] as? Int ?: 0
                totalThirdFailNum += dailyFailDetail["totalThirdFailNum"] as? Int ?: 0
                totalComponentFailNum += dailyFailDetail["totalComponentFailNum"] as? Int ?: 0
            }
        }
        // 生成这一段时间总的执行失败详情
        val totalFailDetail = mutableMapOf<String, Any>()
        setTotalFailDetail(totalFailDetail, "systemFailDetail", totalSystemFailNum)
        setTotalFailDetail(totalFailDetail, "userFailDetail", totalUserFailNum)
        setTotalFailDetail(totalFailDetail, "thirdFailDetail", totalThirdFailNum)
        setTotalFailDetail(totalFailDetail, "componentFailDetail", totalComponentFailNum)
        return StoreStatisticTrendData(
            totalFailNum = totalFailNum,
            totalFailDetail = totalFailDetail,
            dailyStatisticList = dailyStatisticList
        )
    }

    override fun getStoreErrorCodeInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): StoreErrorCodeInfo {
        return StoreErrorCodeInfo(
            storeCode = storeCode,
            storeType = storeType,
            errorCodes = storeErrorCodeInfoDao.getStoreErrorCodes(dslContext, storeCode, storeType)
        )
    }

    private fun setTotalFailDetail(
        totalFailDetail: MutableMap<String, Any>,
        key: String,
        failNum: Int
    ) {
        totalFailDetail[key] = mapOf(
            NAME to I18nUtil.getCodeLanMessage(messageCode = key, defaultMessage = key),
            FAIL_NUM to failNum
        )
    }

    private fun generateStoreStatistic(
        record: Record7<Int, Int, BigDecimal, Int, Int, String, Boolean>?,
        successRate: Double? = null
    ): StoreStatistic {
        return StoreStatistic(
            downloads = record?.value1() ?: 0,
            commentCnt = record?.value2() ?: 0,
            score = String.format("%.1f", record?.value3()?.toDouble()).toDoubleOrNull(),
            pipelineCnt = record?.value4() ?: 0,
            recentExecuteNum = record?.value5() ?: 0,
            successRate = successRate,
            hotFlag = record?.get(KEY_HOT_FLAG) as? Boolean ?: false
        )
    }

    private fun calculateAndStorage(
        storeType: Byte,
        storeCode: String,
        downloads: Int? = null,
        comments: Int? = null,
        score: Double? = null
    ) {
        // 评论均分
        val scoreAverage: Double? = if (comments != null && score != null) {
            if (score > 0 && comments > 0) {
                score.div(comments)
            } else 0.toDouble()
        } else null
        // 统计最近组件执行次数
        val endTime = LocalDateTime.now()
        val dailyStatisticList = storeDailyStatisticService.getDailyStatisticListByCode(
            storeCode = storeCode,
            storeType = storeType,
            startTime = DateTimeUtil.convertDateToLocalDateTime(
                DateTimeUtil.getFutureDate(
                    localDateTime = LocalDateTime.now(),
                    unit = Calendar.MONTH,
                    timeSpan = timeSpanMonth
                )
            ),
            endTime = endTime
        )
        var totalExecuteNum = 0
        dailyStatisticList?.forEach { dailyStatistic ->
            totalExecuteNum += dailyStatistic.dailySuccessNum
            totalExecuteNum += dailyStatistic.dailyFailNum
        }
        val statisticTotal = storeStatisticTotalDao.getStatisticByStoreCode(dslContext, storeCode, storeType)
        val percentileValue =
            redisOperation.get("STORE_${StoreTypeEnum.getStoreType(storeType.toInt())}_PERCENTILE_VALUE")
        if (statisticTotal != null) {
            storeStatisticTotalDao.updateStatisticData(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                downloads = downloads,
                comments = comments,
                score = score?.toInt(),
                scoreAverage = scoreAverage,
                recentExecuteNum = totalExecuteNum,
                hotFlag = percentileValue?.let { totalExecuteNum >= percentileValue.toDouble() }
            )
        } else {
            storeStatisticTotalDao.initStatisticData(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                downloads = downloads,
                comments = comments,
                score = score?.toInt(),
                scoreAverage = scoreAverage,
                recentExecuteNum = totalExecuteNum,
                hotFlag = percentileValue?.let { totalExecuteNum >= percentileValue.toDouble() }
            )
        }
    }

    /**
     * 百分位数计算
     */
    private fun percentileCalculation(storeType: StoreTypeEnum, percentile: Double): Double {
        var value = 0.0
        // 获取组件类型的总数
        val count = storeStatisticTotalDao.getCountByType(dslContext, storeType)
        // 计算出要查找的百分位索引
        val index = (count + 1) * percentile
        if (index >= 1) {
            value += storeStatisticTotalDao.getStorePercentileValue(
                dslContext = dslContext,
                storeType = storeType,
                index = index.roundToInt()
            )?.value1() ?: 0
            if ((index % 1) != 0.0) {
                // 如果索引不是整数，则取平均值
                value += storeStatisticTotalDao.getStorePercentileValue(
                    dslContext = dslContext,
                    storeType = storeType,
                    index = index.roundToInt() + 1
                )?.value1() ?: 0
                value /= 2
            }
        }

        return value
    }
}
