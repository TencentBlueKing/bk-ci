package com.tencent.devops.store.common.service

import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

abstract class AbstractStoreComponentPkgSizeHandleService {

    /**
     * 处理商城组件包大小
     */
    abstract fun batchUpdateComponentsVersionSize()

    abstract fun updateComponentVersionSize(
        storeId: String,
        storePackageInfoReqs: List<StorePackageInfoReq>,
        storeType: StoreTypeEnum
    ):Boolean

}