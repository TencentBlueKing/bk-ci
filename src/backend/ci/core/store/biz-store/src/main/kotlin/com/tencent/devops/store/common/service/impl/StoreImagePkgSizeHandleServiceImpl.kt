package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.image.dao.ImageCommonDao
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service("IMAGE_PKG_SIZE_HANDLE_SERVICE")
class StoreImagePkgSizeHandleServiceImpl : AbstractStoreComponentPkgSizeHandleService() {

    @Autowired
    lateinit var imageDao: ImageCommonDao

    @Autowired
    lateinit var dslContext: DSLContext

    override fun batchUpdateComponentsVersionSize() {
        TODO("Not yet implemented")
    }

    override fun updateComponentVersionSize(
        storeId: String,
        storePackageInfoReqs: List<StorePackageInfoReq>,
        storeType: StoreTypeEnum
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getComponentVersionSize(version: String, storeCode: String): BigDecimal? {
        return imageDao.getComponentSizeByVersionAndCode(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version
        ).takeIf { !it.isNullOrBlank() }
            ?.let { formatSizeInMB(BigDecimal(it)) }
    }
}