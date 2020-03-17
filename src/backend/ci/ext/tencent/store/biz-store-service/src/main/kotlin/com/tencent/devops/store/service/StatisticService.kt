package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.store.dao.ExtStoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.ExtServiceInstallTrendReq
import com.tencent.devops.store.pojo.ExtServiceStatistic
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class StatisticService @Autowired constructor(
    val storeStatisticDao: StoreStatisticDao,
    val storeProjectRelDao: ExtStoreProjectRelDao,
    val dslContext: DSLContext
) {

    fun getStatisticByServiceCode(serviceCode: String): Result<ExtServiceStatistic> {
        val record = storeStatisticDao.getStatisticByStoreCode(dslContext, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
        val extStatistic = formatAtomStatistic(record, serviceCode)
        return Result(extStatistic)
    }

    fun getInstallTrend(serviceCode: String, days: Long): Result<List<ExtServiceInstallTrendReq>> {
        val startTime: Long = if (days > 30) {
            LocalDateTime.now().timestamp() - TimeUnit.DAYS.toSeconds(30)
        } else {
            LocalDateTime.now().timestamp() - TimeUnit.DAYS.toSeconds(days)
        }
        logger.info("getInstallTrend startTime[$startTime], serviceCode[$serviceCode]")
        val installRecords = storeProjectRelDao.getStoreInstall(
            dslContext = dslContext,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            startTime = startTime
        )
            ?: return Result(emptyList())

        val installTrendList = mutableListOf<ExtServiceInstallTrendReq>()
        val installDayMap = mutableMapOf<String, Int>()
        installRecords.forEach {
            val projectCreateTime = it.createTime.dayOfYear.toString()
            if (installDayMap.containsKey(projectCreateTime)) {
                var count = installDayMap[projectCreateTime]
                installDayMap[projectCreateTime] = count!! + 1
            } else {
                installDayMap[projectCreateTime] = 1
            }
        }

        installDayMap.forEach { (day, count) ->
            installTrendList.add(
                ExtServiceInstallTrendReq(
                    installCount = count,
                    day = day
                )
            )
        }
        return Result(installTrendList)
    }

    private fun formatAtomStatistic(record: Record4<BigDecimal, BigDecimal, BigDecimal, String>, serviceCode: String): ExtServiceStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double = if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()

        return ExtServiceStatistic(
            downloads = downloads ?: 0,
            commentCnt = comments ?: 0,
            score = String.format("%.1f", averageScore).toDoubleOrNull()
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}