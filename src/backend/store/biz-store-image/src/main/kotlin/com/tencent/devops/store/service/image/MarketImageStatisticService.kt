package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.ImageStatistic
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