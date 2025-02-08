package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import org.jooq.Record3
import java.time.LocalDateTime

abstract class StoreComponentVersonLogService {

    companion object {
        val HAS_TAG = setOf(StoreTypeEnum.ATOM)
    }

    abstract fun getStoreComponentVersionLogs(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>>


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
            tag = generateTag(storeType, record.value1(), record.value3())
        )
    }

    private fun generateTag(storeType: StoreTypeEnum, version: String, updateTime: LocalDateTime): String {
        return if (storeType in HAS_TAG) {
            val updateTimeStr = DateTimeUtil.formatDate(
                DateTimeUtil.convertLocalDateTimeToDate(updateTime),
                DateTimeUtil.YYYY_MM_DD
            )
            "prod-v${version}-$updateTimeStr"
        } else {
            " "
        }
    }


}