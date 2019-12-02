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

import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StoreTotalStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao
) : StoreTotalStatisticService {
    companion object {
        private val logger = LoggerFactory.getLogger(StoreTotalStatisticServiceImpl::class.java)
    }

    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    fun stat() {
        val taskName = "StoreTotalStatisticTask"
        logger.info("$taskName:stat:start")
        var storeType = StoreTypeEnum.ATOM.type.toByte()
        calculateAndStorage(
            storeType = storeType,
            statistics = storeStatisticDao.batchGetStatisticByStoreCode(
                dslContext = dslContext,
                storeCodeList = listOf(),
                storeType = storeType
            ),
            interfaceName = taskName
        )

        storeType = StoreTypeEnum.TEMPLATE.type.toByte()
        calculateAndStorage(
            storeType = storeType,
            statistics = storeStatisticDao.batchGetStatisticByStoreCode(
                dslContext = dslContext,
                storeCodeList = listOf(),
                storeType = storeType
            ),
            interfaceName = taskName
        )

        storeType = StoreTypeEnum.IMAGE.type.toByte()
        calculateAndStorage(
            storeType = storeType,
            statistics = storeStatisticDao.batchGetStatisticByStoreCode(
                dslContext = dslContext,
                storeCodeList = listOf(),
                storeType = storeType
            ),
            interfaceName = taskName
        )
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

    private fun calculateAndStorage(
        storeType: Byte,
        statistics: Result<Record4<BigDecimal, BigDecimal, BigDecimal, String>>,
        interfaceName: String? = "Anon"
    ) {
        statistics.forEach {
            //下载量
            val downloads = it.value1().toInt()
            //评论数量
            val comments = it.value2().toInt()
            //评论总分
            val score = it.value3().toDouble()
            val code = it.value4().toString()
            //评论均分
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