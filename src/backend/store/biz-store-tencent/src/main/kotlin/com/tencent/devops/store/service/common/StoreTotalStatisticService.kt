package com.tencent.devops.store.service.common

import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StoreTotalStatisticService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao
) {

    @Scheduled(cron = "0 * * * * ?") // 每小时执行一次
    fun stat() {
        var storeType = StoreTypeEnum.ATOM.type.toByte()
        calculateAndStorage(storeType, storeStatisticDao.batchGetStatisticByStoreCode(dslContext, listOf(), storeType))

        storeType = StoreTypeEnum.TEMPLATE.type.toByte()
        calculateAndStorage(storeType, storeStatisticDao.batchGetStatisticByStoreCode(dslContext, listOf(), storeType))
    }

    fun updateStoreTotalStatisticByCode(storeCode: String, storeType: Byte) {
        calculateAndStorage(storeType, storeStatisticDao.batchGetStatisticByStoreCode(dslContext, listOf(storeCode), storeType))
    }

    private fun calculateAndStorage(storeType: Byte, statistics: Result<Record4<BigDecimal, BigDecimal, BigDecimal, String>>) {
        statistics.forEach {
            val downloads = it.value1().toInt()
            val comments = it.value2().toInt()
            val score = it.value3().toDouble()
            val code = it.value4().toString()
            val scoreAverage: Double = if (score > 0 && comments > 0) score.div(comments) else 0.toDouble()
            storeStatisticTotalDao.updateStatisticData(dslContext, code, storeType, downloads, comments, score.toInt(), scoreAverage)
        }
    }
}