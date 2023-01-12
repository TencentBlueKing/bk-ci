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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.model.store.tables.records.TStoreIndexElementDetailRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexResultRecord
import com.tencent.devops.model.store.tables.records.TStoreStatisticsDailyRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreConstants.DELETE_STORE_INDEX_RESULT_KEY
import com.tencent.devops.store.constant.StoreConstants.DELETE_STORE_INDEX_RESULT_LOCK_KEY
import com.tencent.devops.store.constant.StoreConstants.STORE_DAILY_FAIL_DETAIL
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.StoreIndexManageInfoDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.pojo.common.enums.IndexOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import okhttp3.Request
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TxStoreIndexCronService(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val storeIndexManageInfoDao: StoreIndexManageInfoDao,
    private val atomDao: AtomDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao,
    private val client: Client
) {
    @Value("\${codecc.host:#{null}}")
    private lateinit var codeccHost: String

    /**
     * 执行删除组件指标存量数据
     */
    @Scheduled(cron = "0 * * * * ?") // 每小时执行一次
    fun deleteStoreIndexResult() {
        logger.info("deleteStoreIndexResult cron starts")
        val lock = RedisLock(redisOperation, DELETE_STORE_INDEX_RESULT_LOCK_KEY, 60L)
        try {
            lock.lock()
            redisOperation.getSetMembers(DELETE_STORE_INDEX_RESULT_KEY)?.forEach {
                logger.info("expired indexId is: {}", it)
                storeIndexManageInfoDao.deleteStoreIndexResulById(dslContext, it)
                storeIndexManageInfoDao.deleteStoreIndexElementById(dslContext, it)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to offline index: {}", ignored)
        } finally {
            lock.unlock()
        }
    }

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
            indexOperationType = IndexOperationTypeEnum.PLATFORM,
            indexCode = indexCode
        )
        logger.info("computeAtomSlaIndexData storeIndexBaseInfo is $storeIndexBaseInfoId")
        if (storeIndexBaseInfoId.isNullOrBlank()) {
            return
        }
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
                val atomCodes = atomDao.getPublishedAtoms(
                    dslContext = dslContext,
                    timeDescFlag = false,
                    page = page,
                    pageSize = DEFAULT_PAGE_SIZE
                )
                val tStoreIndexResultRecords = mutableListOf<TStoreIndexResultRecord>()
                val tStoreIndexElementDetailRecords = mutableListOf<TStoreIndexElementDetailRecord>()
                atomCodes.forEach { atomCode ->
                    val dailyStatisticRecordList = storeStatisticDailyDao.getDailyStatisticListByCode(
                        dslContext = dslContext,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte(),
                        startTime = startTime,
                        endTime = endTime
                    )
                    val atomTotalComponentFailCount = atomTotalComponentFailCount(dailyStatisticRecordList)
                    val atomExecuteCountByCode = storeStatisticDailyDao.getAtomExecuteCountByCode(
                        dslContext = dslContext,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte(),
                        startTime = startTime,
                        endTime = endTime
                    )
                    val atomSlaIndexValue =
                        (1 - (atomTotalComponentFailCount.toDouble() / atomExecuteCountByCode.toDouble())) * 100
                    val result = if (atomSlaIndexValue > 99.9) "达标" else "不达标"
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
                    tStoreIndexResultRecord.iconTips =
                        "<span style=\"line-height: 18px\"><span>插件SLA : $elementValue%($result);</span>"
                    tStoreIndexResultRecord.levelId = indexLevelInfo?.id
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
                indexOperationType = IndexOperationTypeEnum.PLATFORM,
                storeType = StoreTypeEnum.ATOM,
                indexCode = indexCode
            )
        logger.info("computeAtomQualityIndexInfo storeIndexBaseInfo is $storeIndexBaseInfoId")
        if (storeIndexBaseInfoId.isNullOrBlank()) {
            return
        }
        val totalTaskNum = atomDao.getPublishedAtomCount(dslContext)
        var finishTaskNum = 0
        val lock = RedisLock(redisOperation, "computeAtomQualityIndexInfo", 60L)
        try {
            lock.lock()
            val startTime = LocalDateTime.now().minusMonths(1)
            val endTime = LocalDateTime.now()
            var projectMinId = client.get(ServiceProjectResource::class).getMinId().data
            val projectMaxId = client.get(ServiceProjectResource::class).getMaxId().data
            val querySize = 10
            logger.info("begin computeAtomSlaIndexData!!")
            do {
                val projectIds = client.get(ServiceProjectResource::class).getProjectListById(
                    minId = projectMinId!!,
                    maxId = projectMinId + querySize
                ).data ?: continue
                val complianceInfo = client.get(ServiceMetricsResource::class).queryAtomComplianceInfo(
                    userId = SYSTEM_USER,
                    projectIds = projectIds.map { it.englishName },
                    startDateTime = startTime,
                    endDateTime = endTime
                ).data
                val tStoreIndexResultRecords = mutableListOf<TStoreIndexResultRecord>()
                val tStoreIndexElementDetailRecords = mutableListOf<TStoreIndexElementDetailRecord>()
                complianceInfo?.forEach { (atomCode, v) ->
                    val elementValue = if (v > 0) String.format("%.2f", v) else "0.0"
                    val codeccOpensourceMeasurement = getCodeccOpensourceMeasurement(atomCode)
                    val result = if (v > 99.9 && codeccOpensourceMeasurement == 100.0) "达标" else "不达标"
                    val indexLevelInfo = storeIndexManageInfoDao.getStoreIndexLevelInfo(
                        dslContext,
                        storeIndexBaseInfoId,
                        result
                    )
                    val tStoreIndexElementDetailRecord1 = TStoreIndexElementDetailRecord()
                    tStoreIndexElementDetailRecord1.id = UUIDUtil.generate()
                    tStoreIndexElementDetailRecord1.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexElementDetailRecord1.storeCode = atomCode
                    tStoreIndexElementDetailRecord1.indexCode = indexCode
                    tStoreIndexElementDetailRecord1.elementName = "错误码合规率"
                    tStoreIndexElementDetailRecord1.elementValue = elementValue
                    tStoreIndexElementDetailRecord1.indexId = storeIndexBaseInfoId
                    val tStoreIndexElementDetailRecord2 = TStoreIndexElementDetailRecord()
                    tStoreIndexElementDetailRecord1.id = UUIDUtil.generate()
                    tStoreIndexElementDetailRecord2.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexElementDetailRecord1.storeCode = atomCode
                    tStoreIndexElementDetailRecord1.indexCode = indexCode
                    tStoreIndexElementDetailRecord1.elementName = "codecc代码质量"
                    tStoreIndexElementDetailRecord1.elementValue = "$codeccOpensourceMeasurement"
                    tStoreIndexElementDetailRecord1.indexId = storeIndexBaseInfoId
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
                                "<span>合规率 : $v%(${if (v > 99.9) "达标" else "不达标"});</span>" +
                                "</br><span>codecc代码得分 : $codeccOpensourceMeasurement" +
                                "(${if (codeccOpensourceMeasurement == 100.0) "达标" else "不达标"})</span></span>"
                    tStoreIndexResultRecord.levelId = indexLevelInfo?.id
                    tStoreIndexResultRecords.add(tStoreIndexResultRecord)
                }
                storeIndexManageInfoDao.batchCreateStoreIndexResult(dslContext, tStoreIndexResultRecords)
                storeIndexManageInfoDao.batchCreateElementDetail(dslContext, tStoreIndexElementDetailRecords)
                // 记录计算进度
                finishTaskNum += projectIds.size
                storeIndexManageInfoDao.updateIndexCalculateProgress(
                    dslContext = dslContext,
                    indexId = storeIndexBaseInfoId,
                    totalTaskNum = totalTaskNum,
                    finishTaskNum = finishTaskNum
                )
                projectMinId += (querySize + 1)
            } while (projectMinId!! <= projectMaxId!!)
            logger.info("end computeAtomSlaIndexData!!")
        } catch (ignored: Throwable) {
            logger.warn("computeAtomSlaIndexData failed", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun atomTotalComponentFailCount(dailyStatisticRecordList: Result<TStoreStatisticsDailyRecord>?): Int {
        var atomTotalThirdFailNum = 0
        dailyStatisticRecordList?.forEach { it ->
            val dailyFailDetail = JsonUtil.toMap(it.get(STORE_DAILY_FAIL_DETAIL) as String)
            dailyFailDetail["totalComponentFailNum"]?.let { totalComponentFailNum ->
                atomTotalThirdFailNum += totalComponentFailNum as Int
            }
        }
        return atomTotalThirdFailNum
    }

    private fun getCodeccOpensourceMeasurement(atomCode: String): Double {
        val atomCodeSrc = atomDao.getAtomCodeSrc(dslContext, atomCode)
        val url = "http://$codeccHost/ms/defect/api/service/defect/opensource/measurement?url=$atomCodeSrc"
        val httpReq = Request.Builder()
            .url(url)
            .get()
            .build()
        OkhttpUtils.doHttp(httpReq).use { response ->
            val body = response.body!!.string()
            logger.info("codecc blueShield response: $body")
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    errorCode = response.code.toString(),
                    defaultMessage = "get codecc opensource measurement response fail $body"
                )
            }
            return JsonUtil.toMap(body)["rdIndicatorsScore"] as Double
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreIndexCronService::class.java)
        private const val DEFAULT_PAGE_SIZE = 10
    }
}