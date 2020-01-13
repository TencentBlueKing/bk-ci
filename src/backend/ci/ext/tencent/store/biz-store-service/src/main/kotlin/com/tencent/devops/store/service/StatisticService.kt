package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.ExtServiceStatistic
import com.tencent.devops.store.pojo.atom.AtomStatistic
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record4
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StatisticService @Autowired constructor(
    val storeStatisticDao: StoreStatisticDao,
    val dslContext: DSLContext
) {

    fun getStatisticByServiceCode(serviceCode: String): Result<ExtServiceStatistic> {
        val record = storeStatisticDao.getStatisticByStoreCode(dslContext, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
        val extStatistic = formatAtomStatistic(record, serviceCode)
        return Result(extStatistic)
    }

    private fun formatAtomStatistic(record: Record4<BigDecimal, BigDecimal, BigDecimal, String>, serviceCode: String): ExtServiceStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double = if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()

        return ExtServiceStatistic(
            downloads = downloads ?: 0,
            commentCnt = comments ?: 0,
            score = String.format("%.1f", averageScore).toDoubleOrNull(),
            serviceCode = serviceCode
        )
    }
}