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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.common.StoreCommentDao
import com.tencent.devops.store.pojo.common.ScoreItemInfo
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreStatisticService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeCommentDao: StoreCommentDao
) : StoreStatisticService {

    private val logger = LoggerFactory.getLogger(StoreStatisticServiceImpl::class.java)

    override fun getStoreCommentScoreInfo(storeCode: String, storeType: StoreTypeEnum): Result<StoreCommentScoreInfo> {
        logger.info("the storeCode is:$storeCode,storeType is:$storeType")
        val scoreInfos = storeCommentDao.getStoreCommentScoreInfo(dslContext, storeCode, storeType.type.toByte())
        logger.info("the scoreInfos is:$scoreInfos")
        val scoreItemList = mutableListOf<ScoreItemInfo>()
        var totalCommentNum = 0L
        var totalScore = 0L
        scoreInfos?.map {
            val score = it["score"] as Int
            val num = it["num"] as Int
            scoreItemList.add(
                ScoreItemInfo(score, num)
            )
            totalCommentNum += num
            totalScore += score * num
        }
        val avgScore: Double =
            if (totalScore > 0 && totalCommentNum > 0) totalScore.toDouble().div(totalCommentNum) else 0.toDouble()
        logger.info("the avgScore is:$avgScore,totalCommentNum is:$totalCommentNum,scoreItemList is:$scoreItemList")
        return Result(
            StoreCommentScoreInfo(
                avgScore = String.format("%.1f", avgScore).toDouble(),
                totalNum = totalCommentNum,
                scoreItemList = scoreItemList
            )
        )
    }
}