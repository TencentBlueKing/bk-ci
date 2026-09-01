package com.tencent.devops.store.common.service.impl

import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import com.tencent.devops.store.pojo.extservice.enums.ExtServiceStatusEnum
import com.tencent.devops.store.service.dao.ServiceCommonDao
import java.math.BigDecimal
import kotlin.collections.get
import kotlin.text.toByte
import kotlin.toString
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service("SERVICE_PKG_SIZE_HANDLE_SERVICE")
class StoreServicePkgSizeHandleServiceImpl : AbstractStoreComponentPkgSizeHandleService() {

    @Autowired
    lateinit var serviceDao: ServiceCommonDao

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var redisOperation: RedisOperation

    override fun batchUpdateComponentsVersionSize() {
        val count = serviceDao.countComponent(dslContext, ExtServiceStatusEnum.RELEASED.status.toByte())
        var offset = 0L
        val bathSize = 100L

        while (offset < count) {
            val storeIds = serviceDao.selectComponentIds(
                dslContext = dslContext,
                offset = offset,
                batchSize = bathSize
            )
            if (storeIds.isNullOrEmpty()) {
                break
            }
            offset += bathSize
            val atomEnvInfos =
                serviceDao.selectComponentEnvInfoByStoreIds(dslContext, storeIds)
            val tExtensionServiceEnvInfo = TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO
            if (!atomEnvInfos.isNullOrEmpty()) {
                atomEnvInfos.forEach {
                    val path = it.get(tExtensionServiceEnvInfo.PKG_PATH).toString();
                    val serviceId = it.get(tExtensionServiceEnvInfo.SERVICE_ID).toString();
                    val nodeSize = client.get(ServiceArchiveComponentPkgResource::class)
                        .getFileSize(StoreTypeEnum.SERVICE, path).data

                    serviceDao.updateComponentVersionInfo(
                        dslContext = dslContext,
                        storeId = serviceId,
                        pkgSize = nodeSize.toString()
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
        serviceDao.updateComponentVersionInfo(
            dslContext = dslContext,
            storeId = storeId,
            pkgSize = storePackageInfoReqs[0].size.toString()
        )
        return true
    }

    override fun getComponentVersionSize(
        version: String,
        storeCode: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo {
        val size = serviceDao.getComponentSizeByVersionAndCode(
            dslContext = dslContext,
            version = version,
            storeCode = storeCode
        )
            .takeIf { !it.isNullOrBlank() }
            ?.let { formatSizeInMB(BigDecimal(it)) }
        return StoreVersionSizeInfo(
            storeCode = storeCode,
            storeType = StoreTypeEnum.SERVICE.name,
            version = version,
            packageSize = size,
            unit = "MB"
        )
    }
}
