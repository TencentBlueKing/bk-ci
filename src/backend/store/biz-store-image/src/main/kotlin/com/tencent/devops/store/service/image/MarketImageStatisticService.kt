/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageStatistic
import org.jooq.DSLContext
import org.jooq.Record4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class MarketImageStatisticService @Autowired constructor (
    private val dslContext: DSLContext,
    private val storeStatisticDao: StoreStatisticDao
) {
    private val logger = LoggerFactory.getLogger(MarketImageStatisticService::class.java)

    /**
     * 根据镜像标识获取统计数据
     */
    fun getStatisticByCode(userId: String, imageCode: String): Result<ImageStatistic> {
        logger.info("the userId is:$userId,imageCode is:$imageCode")
        val record = storeStatisticDao.getStatisticByStoreCode(dslContext, imageCode, StoreTypeEnum.IMAGE.type.toByte())
        val statistic = formatImageStatistic(record)
        return Result(statistic)
    }

    private fun formatImageStatistic(record: Record4<BigDecimal, BigDecimal, BigDecimal, String>): ImageStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double = if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()
        logger.info("the averageScore is:$averageScore")
        return ImageStatistic(
            downloads = downloads ?: 0,
            commentCnt = comments ?: 0,
            score = String.format("%.1f", averageScore).toDoubleOrNull()
        )
    }

    /**
     * 根据批量镜像标识获取统计数据
     */
    fun getStatisticByCodeList(imageCodeList: List<String>): Result<HashMap<String, ImageStatistic>> {
        logger.info("the imageCodeList is:$imageCodeList")
        val records = storeStatisticDao.batchGetStatisticByStoreCode(dslContext, imageCodeList, StoreTypeEnum.IMAGE.type.toByte())
        val statistic = hashMapOf<String, ImageStatistic>()
        records.map {
            if (it.value4() != null) {
                val code = it.value4()
                statistic[code] = formatImageStatistic(it)
            }
        }
        logger.info("the records is:$records")
        return Result(statistic)
    }
}