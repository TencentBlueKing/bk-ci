package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreComponentVersonLogService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionLogs
import org.jooq.DSLContext
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


    override fun getStoreComponentVersionLogs(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ):  Result<StoreVersionLogs> {

        val storeCommonDao = getStoreCommonDao(storeType.name)
        val storeVersionLogList =
            storeVersionLogDao.getStoreComponentVersionLogs(dslContext, storeCode, storeType.type.toByte())

        val versionLogInfos = storeVersionLogList?.takeIf { it.isNotEmpty }
            ?.map { createStoreVersionLogInfo(it, storeType) }
            ?: storeCommonDao.getStoreComponentVersionLogs(dslContext, storeCode)
                ?.map { createStoreVersionLogInfo(it, storeType) }
            ?: emptyList()

        return Result(StoreVersionLogs(versionLogInfos.size, versionLogInfos))
    }


}


