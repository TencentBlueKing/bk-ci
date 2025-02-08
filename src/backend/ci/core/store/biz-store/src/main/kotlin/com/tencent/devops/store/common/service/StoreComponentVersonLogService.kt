package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import org.jooq.Record
import org.jooq.Record4
import java.time.LocalDateTime
import java.util.*

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
        record: Record,
        storeType: StoreTypeEnum
    ): StoreVersionLogInfo {
        return StoreVersionLogInfo(
            version = record.get("VERSION") as? String,
            updateLog = record.get("CONTENT") as? String,
            lastUpdateTime = DateTimeUtil.formatDate(
                DateTimeUtil.convertLocalDateTimeToDate(record.get("UPDATED_TIME") as LocalDateTime),
                DateTimeUtil.YYYY_MM_DD_HH_MM_SS
            ),
            tag = generateTag(storeType, record.get("VERSION") as? String, record.get("releaseTime") as? LocalDateTime)
        )
    }

    private fun generateTag(storeType: StoreTypeEnum, version: String?, releaseTime: LocalDateTime?): String {
        return if (storeType in HAS_TAG) {
            val date = DateTimeUtil.formatDate(
                DateTimeUtil.convertLocalDateTimeToDate(releaseTime!!),
                DateTimeUtil.YYYY_MM_DD_HH_MM_SS
            )
            "prod-v${version}-$date"
        } else {
            " "
        }
    }


}