package com.tencent.devops.store.common.service

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface TxStorePkgService {

    fun updatePackageSha256(
        userId: String,
        storeType: StoreTypeEnum?,
        pageSize: Int? = 100
    )
}