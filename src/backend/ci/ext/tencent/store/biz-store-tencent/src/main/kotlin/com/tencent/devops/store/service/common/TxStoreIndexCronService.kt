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

package com.tencent.devops.store.service.common

import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.metrics.pojo.vo.QueryIntervalVO
import com.tencent.devops.model.store.tables.records.TStoreIndexElementDetailRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexResultRecord
import com.tencent.devops.model.store.tables.records.TStoreStatisticsDailyRecord
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.StoreIndexManageInfoDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.pojo.common.BK_ATOM_SLA
import com.tencent.devops.store.pojo.common.BK_CODE_QUALITY
import com.tencent.devops.store.pojo.common.BK_COMPLIANCE_RATE
import com.tencent.devops.store.pojo.common.BK_NOT_UP_TO_PAR
import com.tencent.devops.store.pojo.common.BK_NO_FAIL_DATA
import com.tencent.devops.store.pojo.common.BK_SUM_DAILY_FAIL_NUM
import com.tencent.devops.store.pojo.common.BK_SUM_DAILY_SUCCESS_NUM
import com.tencent.devops.store.pojo.common.BK_UP_TO_PAR
import com.tencent.devops.store.pojo.common.STORE_DAILY_FAIL_DETAIL
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.math.BigDecimal
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TxStoreIndexCronService(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val storeIndexManageInfoDao: StoreIndexManageInfoDao,
    private val atomDao: AtomDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao,
    private val client: Client
) {

    /**
     * 计算插件SLA指标数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun computeAtomSlaIndexData() {
        logger.info("computeAtomSlaIndexData cron starts")
        val indexCode = "atomSlaIndex"
        val storeIndexBaseInfoId = storeIndexManageInfoDao.getStoreIndexBaseInfo(
            dslContext = dslContext,
            storeType = StoreTypeEnum.ATOM,
            indexCode = indexCode
        )
        // 数据库中存在指标基本信息才开始进行指标计算
        logger.info("computeAtomSlaIndexData storeIndexBaseInfo is $storeIndexBaseInfoId")
        if (storeIndexBaseInfoId.isNullOrBlank()) {
            return
        }
        // 对已发布的插件进行指标计算
        val totalTaskNum = atomDao.getPublishedAtomCount(dslContext)
        var finishTaskNum = 0
        val lock = RedisLock(redisOperation, "computeAtomSlaIndexData", 60L)
        try {
            lock.lock()
            val startTime = LocalDateTime.now().minusMonths(1)
            val endTime = LocalDateTime.now()
            logger.info("begin computeAtomSlaIndexData!!")
            var page = 1
            do {
                // 分页获取已发布的插件
                val atomCodes = atomDao.getPublishedAtoms(
                    dslContext = dslContext,
                    timeDescFlag = false,
                    page = page,
                    pageSize = DEFAULT_PAGE_SIZE
                )
                val tStoreIndexResultRecords = mutableListOf<TStoreIndexResultRecord>()
                val tStoreIndexElementDetailRecords = mutableListOf<TStoreIndexElementDetailRecord>()
                atomCodes.forEach { atomCode ->
                    // 获取时间段内的每日数据进行指标计算
                    val dailyStatisticRecordList = storeStatisticDailyDao.getDailyStatisticListByCode(
                        dslContext = dslContext,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte(),
                        startTime = startTime,
                        endTime = endTime
                    )
                    val atomTotalComponentFailCount = atomTotalComponentFailCount(dailyStatisticRecordList)
                    val storeExecuteCountByCodeRecord = storeStatisticDailyDao.getStoreExecuteCountByCode(
                        dslContext = dslContext,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte(),
                        startTime = startTime,
                        endTime = endTime
                    )
                    var storeExecuteCountByCode = 0
                    if (storeExecuteCountByCodeRecord != null) {
                        val sumDailySuccessNum =
                            (storeExecuteCountByCodeRecord.get(BK_SUM_DAILY_SUCCESS_NUM) as? BigDecimal)?.toInt() ?: 0
                        val sumDailyFailNum =
                            (storeExecuteCountByCodeRecord.get(BK_SUM_DAILY_FAIL_NUM) as? BigDecimal)?.toInt() ?: 0
                        storeExecuteCountByCode = sumDailyFailNum + sumDailySuccessNum
                    }
                    // sla计算
                    val atomSlaIndexValue =
                        if (storeExecuteCountByCode == 0) {
                            0.0
                        } else {
                            (1 - (atomTotalComponentFailCount.toDouble() / storeExecuteCountByCode.toDouble())) * 100
                        }
                    val result = if (atomSlaIndexValue > 99.9) BK_UP_TO_PAR else BK_NOT_UP_TO_PAR
                    val indexLevelInfo = storeIndexManageInfoDao.getStoreIndexLevelInfo(
                        dslContext,
                        storeIndexBaseInfoId,
                        result
                    )
                    val elementValue = if (atomSlaIndexValue > 0) String.format("%.2f", atomSlaIndexValue) else "0.0"
                    val tStoreIndexResultRecord = TStoreIndexResultRecord()
                    tStoreIndexResultRecord.id = UUIDUtil.generate()
                    tStoreIndexResultRecord.storeCode = atomCode
                    tStoreIndexResultRecord.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexResultRecord.indexId = storeIndexBaseInfoId
                    tStoreIndexResultRecord.indexCode = indexCode
                    tStoreIndexResultRecord.levelId = indexLevelInfo?.id
                    tStoreIndexResultRecord.iconTips =
                        "<span style=\"line-height: 18px\"><span>$BK_ATOM_SLA ： $elementValue%（$result）</span>"
                    tStoreIndexResultRecord.creator = SYSTEM_USER
                    tStoreIndexResultRecord.modifier = SYSTEM_USER
                    tStoreIndexResultRecord.createTime = LocalDateTime.now()
                    tStoreIndexResultRecord.updateTime = LocalDateTime.now()
                    tStoreIndexResultRecords.add(tStoreIndexResultRecord)
                    val tStoreIndexElementDetailRecord = TStoreIndexElementDetailRecord()
                    tStoreIndexElementDetailRecord.id = UUIDUtil.generate()
                    tStoreIndexElementDetailRecord.storeCode = atomCode
                    tStoreIndexElementDetailRecord.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexElementDetailRecord.indexId = storeIndexBaseInfoId
                    tStoreIndexElementDetailRecord.indexCode = indexCode
                    tStoreIndexElementDetailRecord.elementName = indexCode
                    tStoreIndexElementDetailRecord.elementValue = elementValue
                    tStoreIndexElementDetailRecord.creator = SYSTEM_USER
                    tStoreIndexElementDetailRecord.modifier = SYSTEM_USER
                    tStoreIndexElementDetailRecord.createTime = LocalDateTime.now()
                    tStoreIndexElementDetailRecord.updateTime = LocalDateTime.now()
                    tStoreIndexElementDetailRecords.add(tStoreIndexElementDetailRecord)
                }
                storeIndexManageInfoDao.batchCreateStoreIndexResult(dslContext, tStoreIndexResultRecords)
                storeIndexManageInfoDao.batchCreateElementDetail(dslContext, tStoreIndexElementDetailRecords)
                // 记录计算进度
                finishTaskNum += atomCodes.size
                storeIndexManageInfoDao.updateIndexCalculateProgress(
                    dslContext = dslContext,
                    indexId = storeIndexBaseInfoId,
                    totalTaskNum = totalTaskNum,
                    finishTaskNum = finishTaskNum
                )
                page++
            } while (atomCodes.size == DEFAULT_PAGE_SIZE)
            logger.info("end computeAtomSlaIndexData!!")
        } catch (ignored: Throwable) {
            logger.warn("computeAtomSlaIndexData failed", ignored)
        } finally {
            lock.unlock()
        }
    }

    /**
     * 计算插件质量指标数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun computeAtomQualityIndexInfo() {
        logger.info("computeAtomQualityIndexInfo cron starts")
        val indexCode = "atomQualityIndex"
        val storeIndexBaseInfoId =
            storeIndexManageInfoDao.getStoreIndexBaseInfo(
                dslContext = dslContext,
                storeType = StoreTypeEnum.ATOM,
                indexCode = indexCode
            )
        logger.info("computeAtomQualityIndexInfo storeIndexBaseInfo is $storeIndexBaseInfoId")
        // 数据库中存在指标基本信息才开始进行指标计算
        if (storeIndexBaseInfoId.isNullOrBlank()) {
            return
        }
        val totalTaskNum = atomDao.getPublishedAtomCount(dslContext)
        var finishTaskNum = 0
        var page = 1
        val pageSize = 100
        val lock = RedisLock(redisOperation, "computeAtomQualityIndexInfo", 60L)
        try {
            lock.lock()
            val startTime = LocalDateTime.now().minusMonths(1)
            val endTime = LocalDateTime.now()
            logger.info("begin computeAtomQualityIndexInfo!!")
            do {
                val atomCodes = atomDao.getPublishedAtoms(
                    dslContext = dslContext,
                    page = page,
                    pageSize = pageSize
                )
                atomCodes.forEach { atomCode ->
                    // 获取插件合规信息
                    val complianceInfo = client.get(ServiceMetricsResource::class).queryAtomComplianceInfo(
                        userId = SYSTEM_USER,
                        atomCode = atomCode,
                        QueryIntervalVO(
                            startDateTime = startTime,
                            endDateTime = endTime
                        )
                    ).data
                    val tStoreIndexResultRecords = mutableListOf<TStoreIndexResultRecord>()
                    val tStoreIndexElementDetailRecords = mutableListOf<TStoreIndexElementDetailRecord>()
                    var complianceRate = 100.0
                    var elementValue = complianceInfo?.let {
                        if (complianceInfo.failExecuteCount != 0) {
                            complianceRate =
                                complianceInfo.failComplianceCount.toDouble() / complianceInfo.failExecuteCount * 100.0
                            String.format("%.2f", complianceRate)
                        } else {
                            null
                        }
                    }
                    val codeccOpensourceMeasurement = getCodeccOpensourceMeasurement(atomCode)
                    val result = if (complianceRate > 99.9 && codeccOpensourceMeasurement == 100.0) BK_UP_TO_PAR
                    else BK_NOT_UP_TO_PAR
                    val indexLevelInfo = storeIndexManageInfoDao.getStoreIndexLevelInfo(
                        dslContext,
                        storeIndexBaseInfoId,
                        result
                    )
                    val indexInfo = if (elementValue.isNullOrBlank()) {
                        elementValue = BK_NO_FAIL_DATA
                        elementValue
                    } else {
                        "$elementValue%(${if (complianceRate > 99.9) BK_UP_TO_PAR else BK_NOT_UP_TO_PAR}）"
                    }
                    val tStoreIndexElementDetailRecord1 = TStoreIndexElementDetailRecord()
                    tStoreIndexElementDetailRecord1.id = UUIDUtil.generate()
                    tStoreIndexElementDetailRecord1.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexElementDetailRecord1.storeCode = atomCode
                    tStoreIndexElementDetailRecord1.indexCode = indexCode
                    tStoreIndexElementDetailRecord1.elementName = BK_COMPLIANCE_RATE
                    tStoreIndexElementDetailRecord1.elementValue = elementValue
                    tStoreIndexElementDetailRecord1.indexId = storeIndexBaseInfoId
                    tStoreIndexElementDetailRecord1.creator = SYSTEM_USER
                    tStoreIndexElementDetailRecord1.modifier = SYSTEM_USER
                    tStoreIndexElementDetailRecord1.createTime = LocalDateTime.now()
                    tStoreIndexElementDetailRecord1.updateTime = LocalDateTime.now()
                    val tStoreIndexElementDetailRecord2 = TStoreIndexElementDetailRecord()
                    tStoreIndexElementDetailRecord2.id = UUIDUtil.generate()
                    tStoreIndexElementDetailRecord2.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexElementDetailRecord2.storeCode = atomCode
                    tStoreIndexElementDetailRecord2.indexCode = indexCode
                    tStoreIndexElementDetailRecord2.elementName = BK_CODE_QUALITY
                    tStoreIndexElementDetailRecord2.elementValue = "$codeccOpensourceMeasurement"
                    tStoreIndexElementDetailRecord2.indexId = storeIndexBaseInfoId
                    tStoreIndexElementDetailRecord2.creator = SYSTEM_USER
                    tStoreIndexElementDetailRecord2.modifier = SYSTEM_USER
                    tStoreIndexElementDetailRecord2.createTime = LocalDateTime.now()
                    tStoreIndexElementDetailRecord2.updateTime = LocalDateTime.now()
                    tStoreIndexElementDetailRecords.add(tStoreIndexElementDetailRecord1)
                    tStoreIndexElementDetailRecords.add(tStoreIndexElementDetailRecord2)
                    val tStoreIndexResultRecord = TStoreIndexResultRecord()
                    tStoreIndexResultRecord.id = UUIDUtil.generate()
                    tStoreIndexResultRecord.indexId = storeIndexBaseInfoId
                    tStoreIndexResultRecord.indexCode = indexCode
                    tStoreIndexResultRecord.storeCode = atomCode
                    tStoreIndexResultRecord.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexResultRecord.iconTips =
                        "<span style=\"line-height: 18px\">" +
                                "<span>$BK_COMPLIANCE_RATE ： $indexInfo" +
                                "</span></br><span>$BK_CODE_QUALITY ： $codeccOpensourceMeasurement" +
                                "（${if (codeccOpensourceMeasurement == 100.0) BK_UP_TO_PAR else BK_NOT_UP_TO_PAR}）" +
                                "</span></span>"
                    tStoreIndexResultRecord.levelId = indexLevelInfo?.id
                    tStoreIndexResultRecord.creator = SYSTEM_USER
                    tStoreIndexResultRecord.modifier = SYSTEM_USER
                    tStoreIndexResultRecord.createTime = LocalDateTime.now()
                    tStoreIndexResultRecord.updateTime = LocalDateTime.now()
                    tStoreIndexResultRecords.add(tStoreIndexResultRecord)
                    storeIndexManageInfoDao.batchCreateStoreIndexResult(dslContext, tStoreIndexResultRecords)
                    storeIndexManageInfoDao.batchCreateElementDetail(dslContext, tStoreIndexElementDetailRecords)
                }
                // 记录计算进度
                finishTaskNum += atomCodes.size
                storeIndexManageInfoDao.updateIndexCalculateProgress(
                    dslContext = dslContext,
                    indexId = storeIndexBaseInfoId,
                    totalTaskNum = totalTaskNum,
                    finishTaskNum = finishTaskNum
                )
                page += 1
            } while (atomCodes.size >= pageSize)
            logger.info("end computeAtomQualityIndexInfo!!")
        } catch (ignored: Throwable) {
            logger.warn("computeAtomQualityIndexInfo failed", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun atomTotalComponentFailCount(dailyStatisticRecordList: Result<TStoreStatisticsDailyRecord>?): Int {
        var atomTotalThirdFailNum = 0
        dailyStatisticRecordList?.forEach { record ->
            record.get(STORE_DAILY_FAIL_DETAIL)?.let {
                val dailyFailDetail = JsonUtil.toMap(it as String)
                dailyFailDetail["totalComponentFailNum"].let { totalComponentFailNum ->
                    atomTotalThirdFailNum += totalComponentFailNum as Int
                }
            }
        }
        return atomTotalThirdFailNum
    }

    private fun getCodeccOpensourceMeasurement(atomCode: String): Double {
        val atomCodeSrc = atomDao.getAtomCodeSrc(dslContext, atomCode)
        if (!atomCodeSrc.isNullOrBlank()) {
            val result = (client.get(ServiceCodeccResource::class)
                .getCodeccOpensourceMeasurement(atomCodeSrc).data?.get("rdIndicatorsScore"))
            return (result as? Double) ?: 0.0
        }
        return 0.0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreIndexCronService::class.java)
        private const val DEFAULT_PAGE_SIZE = 10
    }
}
