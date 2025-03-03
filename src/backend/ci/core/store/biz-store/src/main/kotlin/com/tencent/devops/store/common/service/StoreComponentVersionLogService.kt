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
import org.jooq.Record3
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
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>> {

        var count: Long

        count =
            storeVersionLogDao.countStoreComponentVersionLogs(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )

        val versionLogInfos = if (count > 0) {

            storeVersionLogDao.getStoreComponentVersionLogs(
                dslContext,
                storeCode,
                storeType.type.toByte(),
                page,
                pageSize
            )?.map { createStoreVersionLogInfo(it, storeType) } ?: emptyList()
        } else {
            try {
                count =
                    getStoreCommonDao(storeType.name).countStoreComponentVersionLogs(dslContext, storeCode)
                if (count > 0) {

                    getStoreCommonDao(storeType.name).getStoreComponentVersionLogs(
                        dslContext,
                        storeCode,
                        page,
                        pageSize
                    )?.map { createStoreVersionLogInfo(it, storeType) } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                logger.error(
                    "getStoreComponentVersionLogs error:${
                        e.message
                    }"
                )
                emptyList()
            }
        }


        return Result(Page(count = count, page = page, pageSize = pageSize, records = versionLogInfos))
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    private fun createStoreVersionLogInfo(
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