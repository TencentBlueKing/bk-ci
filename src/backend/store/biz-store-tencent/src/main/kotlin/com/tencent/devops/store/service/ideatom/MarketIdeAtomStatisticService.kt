package com.tencent.devops.store.service.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.atom.AtomStatistic
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class MarketIdeAtomStatisticService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeStatisticDao: StoreStatisticDao
) {
    private val logger = LoggerFactory.getLogger(MarketIdeAtomService::class.java)

    /**
     * 根据标识获取统计数据
     */
    fun getStatisticByCode(userId: String, atomCode: String): Result<AtomStatistic> {
        logger.info("getStatisticByCode userId is:$userId,atomCode is:$atomCode")
        val record = storeStatisticDao.getStatisticByStoreCode(
            dslContext,
            atomCode,
            StoreTypeEnum.IDE_ATOM.type.toByte()
        )
        val atomStatistic = formatAtomStatistic(record)
        logger.info("getStatisticByCode atomStatistic is:$atomStatistic")
        return Result(atomStatistic)
    }

    private fun formatAtomStatistic(record: Record4<BigDecimal, BigDecimal, BigDecimal, String>): AtomStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double = if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()

        return AtomStatistic(
            downloads = downloads ?: 0,
            commentCnt = comments ?: 0,
            score = String.format("%.1f", averageScore).toDoubleOrNull(),
            pipelineCnt = 0
        )
    }

    /**
     * 根据批量标识获取统计数据
     */
    fun getStatisticByCodeList(atomCodeList: List<String>, statFiledList: List<String>): Result<HashMap<String, AtomStatistic>> {
        logger.info("getStatisticByCode atomCodeList is:$atomCodeList,statFiledList is:$statFiledList")
        val records = storeStatisticDao.batchGetStatisticByStoreCode(
            dslContext,
            atomCodeList,
            StoreTypeEnum.IDE_ATOM.type.toByte()
        )
        val atomStatistic = hashMapOf<String, AtomStatistic>()
        records.map {
            if (it.value4() != null) {
                val atomCode = it.value4()
                atomStatistic[atomCode] = formatAtomStatistic(it)
            }
        }
        logger.info("getStatisticByCode atomStatistic is:$atomStatistic")
        return Result(atomStatistic)
    }
}