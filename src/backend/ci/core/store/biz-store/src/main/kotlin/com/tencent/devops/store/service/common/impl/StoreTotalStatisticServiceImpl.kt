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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.common.StoreStatistic
import com.tencent.devops.store.pojo.common.StoreStatisticPipelineNumUpdate
import com.tencent.devops.store.pojo.common.StoreStatisticTrendData
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreDailyStatisticService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Record5
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StoreTotalStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao,
    private val storeDailyStatisticService: StoreDailyStatisticService
) : StoreTotalStatisticService {
    companion object {
        private val logger = LoggerFactory.getLogger(StoreTotalStatisticServiceImpl::class.java)
    }

    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    fun stat() {
        val taskName = "StoreTotalStatisticTask"
        logger.info("$taskName:stat:start")
        StoreTypeEnum.values().forEach { storeType ->
            calculateAndStorage(
                storeType = storeType.type.toByte(),
                statistics = storeStatisticDao.batchGetStatisticByStoreCode(
                    dslContext = dslContext,
                    storeCodeList = listOf(),
                    storeType = storeType.type.toByte()
                ),
                interfaceName = taskName
            )
        }
        logger.info("$taskName:stat:end")
    }

    override fun updateStoreTotalStatisticByCode(storeCode: String, storeType: Byte) {
        calculateAndStorage(
            storeType = storeType,
            statistics = storeStatisticDao.batchGetStatisticByStoreCode(
                dslContext = dslContext,
                storeCodeList = listOf(storeCode),
                storeType = storeType
            )
        )
    }

    override fun getStatisticByCode(
        userId: String,
        storeType: Byte,
        storeCode: String
    ): StoreStatistic {
        val record =
            storeStatisticTotalDao.getStatisticByStoreCode(dslContext, storeCode, storeType)
        return generateStoreStatistic(record)
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
        val atomStatisticMap = hashMapOf<String, StoreStatistic>()
        records.map {
            if (it.value5() != null) {
                val atomCode = it.value5()
                atomStatisticMap[atomCode] = generateStoreStatistic(it)
            }
        }
        return atomStatisticMap
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
        logger.info("getStatisticTrendDataByCode $userId,$storeCode,$storeType,$startTime,$endTime")
        val dailyStatisticList = storeDailyStatisticService.getDailyStatisticListByCode(
            userId = userId,
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
                totalSystemFailNum += dailyFailDetail["dailySystemFailNum"] as Int
                totalUserFailNum += dailyFailDetail["dailyUserFailNum"] as Int
                totalThirdFailNum += dailyFailDetail["dailyThirdFailNum"] as Int
                totalComponentFailNum += dailyFailDetail["dailyComponentFailNum"] as Int
            }
        }
        // 生成这一段时间总的执行失败详情
        val totalFailDetail = mutableMapOf<String, Any>()
        setTotalFailDetail(totalFailDetail, "systemFailDetail", totalSystemFailNum)
        setTotalFailDetail(totalFailDetail, "userFailDetail", totalUserFailNum)
        setTotalFailDetail(totalFailDetail, "thirdFailDetail", totalThirdFailNum)
        setTotalFailDetail(totalFailDetail, "componentFailDetail", totalComponentFailNum)
        return  StoreStatisticTrendData(
            totalFailNum = totalFailNum,
            totalFailDetail = totalFailDetail,
            dailyStatisticList = dailyStatisticList
        )
    }

    private fun setTotalFailDetail(
        totalFailDetail: MutableMap<String, Any>,
        key: String,
        failNum: Int
    ) {
        totalFailDetail[key] = mapOf(NAME to MessageCodeUtil.getCodeLanMessage(key, key), FAIL_NUM to failNum)
    }

    private fun generateStoreStatistic(record: Record5<Int, Int, BigDecimal, Int, String>): StoreStatistic {
        return StoreStatistic(
            downloads = record.value1(),
            commentCnt = record.value2(),
            score = String.format("%.1f", record.value3()?.toDouble()).toDoubleOrNull(),
            pipelineCnt = record.value4()
        )
    }

    private fun calculateAndStorage(
        storeType: Byte,
        statistics: Result<Record4<BigDecimal, BigDecimal, BigDecimal, String>>,
        interfaceName: String? = "Anon"
    ) {
        statistics.forEach {
            // 下载量
            val downloads = it.value1().toInt()
            // 评论数量
            val comments = it.value2().toInt()
            // 评论总分
            val score = it.value3().toDouble()
            val code = it.value4().toString()
            // 评论均分
            val scoreAverage: Double = if (score > 0 && comments > 0) score.div(comments) else 0.toDouble()
            logger.info("$interfaceName:updateStatisticData(${StoreTypeEnum.getStoreType(storeType.toInt())},$code,$downloads,$comments,$score,$scoreAverage)")
            storeStatisticTotalDao.updateStatisticData(
                dslContext = dslContext,
                storeCode = code,
                storeType = storeType,
                downloads = downloads,
                comments = comments,
                score = score.toInt(),
                scoreAverage = scoreAverage
            )
        }
    }
}
