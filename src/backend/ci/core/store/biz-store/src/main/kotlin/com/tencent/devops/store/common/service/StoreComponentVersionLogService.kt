package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

abstract class StoreComponentVersionLogService {

    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var storeBaseQueryDao: StoreBaseQueryDao

    @Autowired
    lateinit var storeVersionLogDao: StoreVersionLogDao

    @Autowired
    lateinit var dslContext: DSLContext

    companion object {
        val HAS_TAG = setOf(StoreTypeEnum.ATOM)
        private val logger = LoggerFactory.getLogger(StoreComponentVersionLogService::class.java)
    }

    fun getStoreComponentVersionLogs(
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>> {

        var count = storeVersionLogDao.countStoreComponentVersionLogs(dslContext, storeCode, storeType.type.toByte())

        if (count == 0L) {
            val commonDao: AbstractStoreCommonDao
            try {
                commonDao = getStoreCommonDao(storeType.name)
            } catch (ignore: Throwable) {
                logger.error("getStoreCommonDao error: ${ignore.message}, storeType: $storeType", ignore)
                return Result(Page(count = 0, page = page, pageSize = pageSize, records = emptyList()))
            }

            count = commonDao.countStoreComponentVersionLogs(dslContext, storeCode)
            if (count == 0L) {
                return Result(Page(count = 0, page = page, pageSize = pageSize, records = emptyList()))
            }
            return fetchLogs(
                dao = commonDao,
                storeCode = storeCode,
                storeType = storeType,
                page = page,
                pageSize = pageSize,
                count = count
            )
        }

        return fetchLogs(storeCode = storeCode, storeType = storeType, page = page, pageSize = pageSize, count = count)
    }

    private fun fetchLogs(
        dao: AbstractStoreCommonDao? = null,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int,
        count: Long
    ): Result<Page<StoreVersionLogInfo>> {
        val versionLogInfos = if (dao == null) {
            storeVersionLogDao.getStoreComponentVersionLogs(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType.type.toByte(),
                page = page,
                pageSize = pageSize
            )?.map { createStoreVersionLogInfo(record = it, storeType = storeType, sizeFlag = false) }
        } else {
            dao.getStoreComponentVersionLogs(
                dslContext = dslContext,
                storeCode = storeCode,
                page = page,
                pageSize = pageSize
            )?.map { createStoreVersionLogInfo(record = it, storeType = storeType) }
        }
        return Result(Page(count = count, page = page, pageSize = pageSize, records = versionLogInfos ?: emptyList()))
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    private fun createStoreVersionLogInfo(
        record: Record,
        storeType: StoreTypeEnum,
        // todo 目前先查询t_atom等表的包大小  暂时不查迁移到T_STORE_BASE表，这个等后续迁移了历史数据再做调整
        sizeFlag: Boolean = true
    ): StoreVersionLogInfo {
        return StoreVersionLogInfo(
            version = record.get("VERSION") as String,
            updateLog = record.get("CONTENT") as? String,
            lastUpdateTime = DateTimeUtil.formatDate(
                DateTimeUtil.convertLocalDateTimeToDate(record.get("UPDATE_TIME") as LocalDateTime),
                DateTimeUtil.YYYY_MM_DD_HH_MM_SS
            ),
            tag = generateTag(
                storeType = storeType,
                version = record.get("VERSION") as String,
                updateTime = record.get("UPDATE_TIME") as LocalDateTime
            ),
            packageSize = if (sizeFlag) getPackageSize(record = record, storeType = storeType) else "",
            publisher = record.get("MODIFIER") as String
        )
    }

    private fun generateTag(storeType: StoreTypeEnum, version: String, updateTime: LocalDateTime): String {
        return if (storeType in HAS_TAG) {
            val updateTimeStr = DateTimeUtil.formatDate(
                DateTimeUtil.convertLocalDateTimeToDate(updateTime),
                DateTimeUtil.YYYY_MM_DD
            )
            "prod-v$version-$updateTimeStr"
        } else {
            " "
        }
    }

    private fun getPackageSize(record: Record, storeType: StoreTypeEnum): String {
        return when (storeType) {
            StoreTypeEnum.ATOM -> {
                record.get("PACKAGE_SIZE") as? String ?: ""
            }

            StoreTypeEnum.IMAGE -> {
                val size = record.get("IMAGE_SIZE") as? String ?: ""
                if (size.isNotEmpty()) {
                    String.format("%.2f MB", size.toLong() / (1024.0 * 1024.0))
                } else {
                    ""
                }
            }

            else -> {
                ""
            }
        }
    }
}