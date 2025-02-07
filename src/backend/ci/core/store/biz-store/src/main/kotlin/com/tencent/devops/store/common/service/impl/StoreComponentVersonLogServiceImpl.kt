package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreComponentVersonLogService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class StoreComponentVersonLogServiceImpl : StoreComponentVersonLogService() {


    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var storeBaseQueryDao: StoreBaseQueryDao

    @Autowired
    lateinit var storeVersionLogDao: StoreVersionLogDao

    @Autowired
    lateinit var dslContext: DSLContext

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentVersonLogServiceImpl::class.java)
    }


    override fun getStoreComponentVersionLogs(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>> {
        var count = 0

        val storeVersionLogList =
            storeVersionLogDao.getStoreComponentVersionLogs(dslContext, storeCode, storeType.type.toByte())

        val versionLogInfos = if (storeVersionLogList?.isNotEmpty == true) {
            count = storeVersionLogList.size
            storeVersionLogDao.getStoreComponentVersionLogs(
                dslContext,
                storeCode,
                storeType.type.toByte(),
                page,
                pageSize
            )?.map { createStoreVersionLogInfo(it, storeType) } ?: emptyList()
        } else {
            try {
                val VersionLogList =
                    getStoreCommonDao(storeType.name).getStoreComponentVersionLogs(dslContext, storeCode)
                if (VersionLogList?.isNotEmpty == true) {
                    count = VersionLogList.size
                    storeVersionLogDao.getStoreComponentVersionLogs(
                        dslContext,
                        storeCode,
                        storeType.type.toByte(),
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


        return Result(Page(count = count.toLong(), page = page, pageSize = pageSize, records = versionLogInfos))
    }


}


