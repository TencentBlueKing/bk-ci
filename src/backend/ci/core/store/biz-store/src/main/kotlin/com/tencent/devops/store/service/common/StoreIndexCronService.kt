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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.store.tables.records.TStoreIndexElementDetailRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexResultRecord
import com.tencent.devops.model.store.tables.records.TStoreStatisticsDailyRecord
import com.tencent.devops.store.constant.StoreConstants.DELETE_STORE_INDEX_RESULT_KEY
import com.tencent.devops.store.constant.StoreConstants.DELETE_STORE_INDEX_RESULT_LOCK_KEY
import com.tencent.devops.store.constant.StoreConstants.STORE_DAILY_FAIL_DETAIL
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.StoreIndexManageInfoDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.pojo.common.enums.IndexOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreIndexCronService constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val storeIndexManageInfoDao: StoreIndexManageInfoDao,
    private val atomDao: AtomDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao
) {


    /**
     * 执行删除组件指标存量数据
     */
    @Scheduled(cron = "0 * * * * ?") // 每小时执行一次
    fun deleteStoreIndexResult() {
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

    @Scheduled(cron = "0 0 1 * * ?")
    /**
     * 计算插件SLA指标数据
     */
    fun computeAtomSlaIndexData() {
        val indexCode = "atomSlaIndex"
        val storeIndexBaseInfo = storeIndexManageInfoDao.getStoreIndexBaseInfo(
            dslContext = dslContext,
            storeType = StoreTypeEnum.ATOM,
            indexOperationType = IndexOperationTypeEnum.PLATFORM,
            indexCode = indexCode
        ) ?: return
        val getPublishedAtomCount = atomDao.getPublishedAtomCount(dslContext)
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
                        storeIndexBaseInfo.id,
                        result
                    )
                    val elementValue = String.format("%.2f", atomSlaIndexValue)
                    val tStoreIndexResultRecord = TStoreIndexResultRecord()
                    tStoreIndexResultRecord.id = UUIDUtil.generate()
                    tStoreIndexResultRecord.storeCode = atomCode
                    tStoreIndexResultRecord.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexResultRecord.indexId = storeIndexBaseInfo.id
                    tStoreIndexResultRecord.indexCode = indexCode
                    tStoreIndexResultRecord.iconTips =
                        "<span style=\"line-height: 18px\"><span>插件SLA : $elementValue%($result);</span>"
                    tStoreIndexResultRecord.levelId = indexLevelInfo?.id
                    tStoreIndexResultRecords.add(tStoreIndexResultRecord)
                    val tStoreIndexElementDetailRecord = TStoreIndexElementDetailRecord()
                    tStoreIndexElementDetailRecord.id = UUIDUtil.generate()
                    tStoreIndexElementDetailRecord.storeCode = atomCode
                    tStoreIndexElementDetailRecord.storeType = StoreTypeEnum.ATOM.type.toByte()
                    tStoreIndexElementDetailRecord.indexId = storeIndexBaseInfo.id
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
                val progress = page * DEFAULT_PAGE_SIZE / getPublishedAtomCount.toDouble() * 100
                redisOperation.sadd(indexCode, String.format("%.2f", progress))
                page++
            } while (atomCodes.size == DEFAULT_PAGE_SIZE)
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


    companion object {
        private val logger = LoggerFactory.getLogger(StoreIndexCronService::class.java)
        private const val DEFAULT_PAGE_SIZE = 10
    }
}