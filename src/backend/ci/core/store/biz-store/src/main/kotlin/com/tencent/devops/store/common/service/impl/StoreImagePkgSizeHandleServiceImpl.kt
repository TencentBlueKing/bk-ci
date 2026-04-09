package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.constant.StoreConstants.MB_UNIT
import com.tencent.devops.store.image.dao.ImageCommonDao
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import java.math.BigDecimal
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("IMAGE_PKG_SIZE_HANDLE_SERVICE")
class StoreImagePkgSizeHandleServiceImpl : AbstractStoreComponentPkgSizeHandleService() {

    @Autowired
    lateinit var imageDao: ImageCommonDao

    @Autowired
    lateinit var dslContext: DSLContext

    companion object {
        private val logger = LoggerFactory.getLogger(StoreImagePkgSizeHandleServiceImpl::class.java)
    }

    override fun batchUpdateComponentsVersionSize() {
        // IMAGE 类型暂不支持批量更新包大小，预留扩展
        logger.info("batchUpdateComponentsVersionSize is not supported for IMAGE type, skip.")
    }

    override fun updateComponentVersionSize(
        storeId: String,
        storePackageInfoReqs: List<StorePackageInfoReq>,
        storeType: StoreTypeEnum
    ): Boolean {
        // IMAGE 类型暂不支持更新包大小，预留扩展
        logger.info("updateComponentVersionSize is not supported for IMAGE type, skip. storeId: $storeId")
        return true
    }

    override fun getComponentVersionSize(
        version: String,
        storeCode: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo {
        val size = imageDao.getComponentSizeByVersionAndCode(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version
        ).takeIf { !it.isNullOrBlank() }
            ?.let { formatSizeInMB(BigDecimal(it)) }

        return StoreVersionSizeInfo(
            storeCode = storeCode,
            storeType = StoreTypeEnum.IMAGE.name,
            version = version,
            packageSize = size,
            unit = MB_UNIT
        )
    }
}