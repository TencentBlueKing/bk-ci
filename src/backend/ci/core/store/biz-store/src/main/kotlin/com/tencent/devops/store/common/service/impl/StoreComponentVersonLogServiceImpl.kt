package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreComponentVersonLogService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogs
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
        storeType: StoreTypeEnum
    ): Result<StoreVersionLogs> {

        val storeVersionLogList =
            storeVersionLogDao.getStoreComponentVersionLogs(dslContext, storeCode, storeType.type.toByte())

        var versionLogInfos = storeVersionLogList?.takeIf { it.isNotEmpty }
            ?.map { createStoreVersionLogInfo(it, storeType) }

        if (versionLogInfos.isNullOrEmpty()) {
            versionLogInfos = try {
                getStoreCommonDao(storeType.name).getStoreComponentVersionLogs(dslContext, storeCode)
                    ?.map { createStoreVersionLogInfo(it, storeType) }
                    ?: emptyList()

            } catch (e: Exception) {
                logger.error(
                    "getStoreComponentVersionLogs error:${
                        e.message
                    }"
                )
                emptyList()
            }


        }

        return Result(StoreVersionLogs(versionLogInfos!!.size, versionLogInfos))
    }


}


