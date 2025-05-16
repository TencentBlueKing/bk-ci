package com.tencent.devops.store.common.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.atom.dao.AtomCommonDao
import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service("ATOM_PKG_SIZE_HANDLE_SERVICE")
class StoreAtomPkgSizeHandleServiceImpl : AbstractStoreComponentPkgSizeHandleService() {

    @Autowired
    lateinit var atomDao: AtomCommonDao

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client


    @Autowired
    lateinit var redisOperation: RedisOperation


    override fun batchUpdateComponentsVersionSize() {
        val count = atomDao.countComponent(dslContext, AtomStatusEnum.RELEASED.status.toByte())
        var offset = 0L
        val bathSize = 100L

        while (offset < count) {

            val storeIds = atomDao.selectComponentIds(
                dslContext = dslContext,
                offset = offset,
                batchSize = bathSize
            )
            if (storeIds.isNullOrEmpty()) {
                break
            }
            offset += bathSize
            val atomEnvInfos =
                atomDao.selectComponentEnvInfoByStoreIds(dslContext, storeIds)
            if (!atomEnvInfos.isNullOrEmpty()) {
                val atomEnvInfosMap = atomEnvInfos.groupBy { it.get("ATOM_ID").toString() }
                atomEnvInfosMap.forEach { (atomId, records) ->
                    val storePackageInfoReqs = mutableListOf<StorePackageInfoReq>()
                    records.forEach {
                        val nodeSize = client.get(ServiceArchiveComponentPkgResource::class)
                            .getFileSize(StoreTypeEnum.ATOM, it.get("PKG_PATH").toString()).data
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
                    atomDao.updateComponentVersionInfo(
                        dslContext = dslContext,
                        atomId,
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
            val size = atomDao.getComponentVersionSizeInfo(dslContext, storeId)
            if (size.isNullOrBlank()) {
                atomDao.updateComponentVersionInfo(
                    dslContext = dslContext,
                    storeId = storeId,
                    pkgSize = JsonUtil.toJson(storePackageInfoReqs)
                )
            } else {
                val atomPackageInfoList = JsonUtil.to(size, object : TypeReference<List<StorePackageInfoReq>>() {})
                val mutableList = atomPackageInfoList.toMutableList()
                mutableList.addAll(storePackageInfoReqs)
                atomDao.updateComponentVersionInfo(
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

    override fun getComponentVersionSize(
        version: String,
        storeCode: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo {
        val size = atomDao.getComponentSizeByVersionAndCode(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version
        ).takeIf { !it.isNullOrBlank() }
            ?.let {
                parseComponentPackageSize(
                    size = it,
                    osName = osName,
                    osArch = osArch
                )
            }
        return StoreVersionSizeInfo(
            storeCode = storeCode,
            storeType = StoreTypeEnum.ATOM.name,
            version = version,
            packageSize = size,
            unit = "MB"
        )
    }
}

