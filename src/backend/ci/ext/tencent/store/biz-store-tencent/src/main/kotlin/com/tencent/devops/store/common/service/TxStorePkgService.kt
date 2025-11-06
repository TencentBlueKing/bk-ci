package com.tencent.devops.store.common.service

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.common.api.pojo.Result

interface TxStorePkgService {

    fun updatePackageSha256(
        userId: String,
        storeType: StoreTypeEnum?,
        pageSize: Int? = 100
    ): Result<Boolean>
}