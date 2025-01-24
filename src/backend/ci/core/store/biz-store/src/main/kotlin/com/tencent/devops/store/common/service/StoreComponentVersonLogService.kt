package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionLogs
import org.jooq.Record3
import java.time.LocalDateTime
import java.util.*

abstract class StoreComponentVersonLogService {

    companion object {
        val HAS_TAG = setOf(StoreTypeEnum.ATOM)
    }

    abstract fun getStoreComponentVersionLogs(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<StoreVersionLogs>


    fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    fun createStoreVersionLogInfo(
        record: Record3<String, String, LocalDateTime>,
        storeType: StoreTypeEnum
    ): StoreVersionLogInfo {
        return StoreVersionLogInfo(
            version = record.value1(),
            updateLog = record.value2(),
            lastUpdateTime = DateTimeUtil.formatDate(
                DateTimeUtil.convertLocalDateTimeToDate(record.value3()),
                DateTimeUtil.YYYY_MM_DD_HH_MM_SS
            ),
            tag = generateTag(storeType, record.value1())
        )
    }

    private fun generateTag(storeType: StoreTypeEnum, version: String): String {
        return if (storeType in HAS_TAG) {
            val date = DateTimeUtil.formatDate(Date(), DateTimeUtil.YYYY_MM_DD)
            "prod-v${version}-$date"
        } else {
            " "
        }
    }


}