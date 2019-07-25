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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.ServiceMeasurePipelineResource
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.atom.AtomStatistic
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomStatisticService
import org.jooq.DSLContext
import org.jooq.Record4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class MarketAtomStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeStatisticDao: StoreStatisticDao,
    private val client: Client
) : MarketAtomStatisticService {
    private val logger = LoggerFactory.getLogger(MarketAtomStatisticServiceImpl::class.java)

    /**
     * 根据插件标识获取统计数据
     */
    override fun getStatisticByCode(userId: String, atomCode: String): Result<AtomStatistic> {
        logger.info("getStatisticByCode userId is :$userId,atomCode is :$atomCode")
        val record = storeStatisticDao.getStatisticByStoreCode(dslContext, atomCode, StoreTypeEnum.ATOM.type.toByte())
        val pipelineCnt =
            client.get(ServiceMeasurePipelineResource::class).getPipelineCountByAtomCode(atomCode).data ?: 0
        val atomStatistic = formatAtomStatistic(record, pipelineCnt)
        logger.info("getStatisticByCode atomStatistic is :$atomStatistic")
        return Result(atomStatistic)
    }

    private fun formatAtomStatistic(
        record: Record4<BigDecimal, BigDecimal, BigDecimal, String>,
        pipelineCnt: Int
    ): AtomStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double =
            if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()

        return AtomStatistic(
            downloads = downloads ?: 0,
            commentCnt = comments ?: 0,
            score = String.format("%.1f", averageScore).toDoubleOrNull(),
            pipelineCnt = pipelineCnt
        )
    }

    /**
     * 根据批量插件标识获取统计数据
     */
    override fun getStatisticByCodeList(
        atomCodeList: List<String>,
        statFiledList: List<String>
    ): Result<HashMap<String, AtomStatistic>> {
        logger.info("getStatisticByCodeList atomCodeList is :$atomCodeList,statFiledList is :$statFiledList")
        val records =
            storeStatisticDao.batchGetStatisticByStoreCode(dslContext, atomCodeList, StoreTypeEnum.ATOM.type.toByte())
        val atomCodes = atomCodeList.joinToString(",")
        val isStatPipeline = statFiledList.contains("PIPELINE")
        val pipelineStat = if (isStatPipeline) {
            client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(atomCodes).data
        } else {
            mutableMapOf()
        }
        val atomStatistic = hashMapOf<String, AtomStatistic>()
        records.map {
            if (it.value4() != null) {
                val atomCode = it.value4()
                atomStatistic[atomCode] = formatAtomStatistic(it, pipelineStat?.get(atomCode) ?: 0)
            }
        }
        logger.info("the atomStatistic is :atomStatistic")
        return Result(atomStatistic)
    }
}