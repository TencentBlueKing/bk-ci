package com.tencent.devops.store.common.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service("DEVX_PKG_SIZE_HANDLE_SERVICE")
class StoreDevxPkgSizeHandleServiceImpl: AbstractStoreComponentPkgSizeHandleService() {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var storeVersionLogDao: StoreVersionLogDao


    override fun batchUpdateComponentsVersionSize() {
        val count = storeVersionLogDao.countComponent(dslContext, StoreTypeEnum.DEVX.type.toByte())
        var offset = 0L
        val bathSize = 100L

        while (offset < count) {

            val storeIds = storeVersionLogDao.selectComponentIds(
                dslContext = dslContext,
                offset = offset,
                batchSize = bathSize
            )
            if (storeIds.isNullOrEmpty()) {
                break
            }
            offset += bathSize
            val atomEnvInfos = storeVersionLogDao.selectComponentEnvInfoByStoreIds(dslContext, storeIds)
            if (!atomEnvInfos.isNullOrEmpty()) {
                val storePackageInfoReqs = mutableListOf<StorePackageInfoReq>()
                val atomEnvInfosMap = atomEnvInfos.groupBy { it.get("STORE_ID").toString() }
                atomEnvInfosMap.forEach { (storeId, records) ->
                    records.forEach {
                        val nodeSize = client.get(ServiceArchiveComponentPkgResource::class)
                            .getFileSize(StoreTypeEnum.DEVX, it.get("PKG_PATH").toString()).data
                        if (nodeSize != null) {
                            storePackageInfoReqs.add(
                                StorePackageInfoReq(
                                    osName = it.get("OS_NAME").toString(),
                                    arch = it.get("OS_ARCH").toString(),
                                    size = nodeSize
                                )
                            )
                        }
                    }
                    storeVersionLogDao.updateComponentVersionInfo(
                        dslContext = dslContext,
                        storeId,
                        JsonUtil.toJson(storePackageInfoReqs)
                    )
                }
            }
        }
    }

    override fun updateComponentVersionSize(
        storeId: String,
        storePackageInfoReqs: List<StorePackageInfoReq>,
        storeType: StoreTypeEnum
    ): Boolean {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "store:$storeId:${storeType.name}",
            expiredTimeInSeconds = 10
        )
        try {
            redisLock.lock()
            val size = storeVersionLogDao.getComponentVersionSizeInfo(dslContext, storeId)
            if (size.isNullOrBlank()) {
                storeVersionLogDao.updateComponentVersionInfo(
                    dslContext = dslContext,
                    storeId = storeId,
                    pkgSize = JsonUtil.toJson(storePackageInfoReqs)
                )
            } else {
                val atomPackageInfoList = JsonUtil.to(size, object : TypeReference<List<StorePackageInfoReq>>() {})
                val mutableList = atomPackageInfoList.toMutableList()
                mutableList.addAll(storePackageInfoReqs)
                storeVersionLogDao.updateComponentVersionInfo(
                    dslContext = dslContext,
                    storeId = storeId,
                    pkgSize = JsonUtil.toJson(mutableList)
                )

            }
        } finally {
            redisLock.unlock()
        }

        return true
    }
}