package com.tencent.devops.store.service.common

import com.tencent.devops.store.dao.common.StoreCommentDao
import com.tencent.devops.store.pojo.common.ScoreItemInfo
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.common.api.pojo.Result
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreStatisticService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeCommentDao: StoreCommentDao
) {
    private val logger = LoggerFactory.getLogger(StoreStatisticService::class.java)

    fun getStoreCommentScoreInfo(storeCode: String, storeType: StoreTypeEnum): Result<StoreCommentScoreInfo> {
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
            totalScore += score*num
        }
        val avgScore: Double = if (totalScore > 0 && totalCommentNum > 0) totalScore.toDouble().div(totalCommentNum) else 0.toDouble()
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