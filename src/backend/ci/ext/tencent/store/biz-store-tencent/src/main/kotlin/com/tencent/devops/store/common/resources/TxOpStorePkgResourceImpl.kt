package com.tencent.devops.store.common.resources

import TxOpStorePkgResource
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.common.service.TxStorePkgService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result
@RestResource
class TxOpStorePkgResourceImpl@Autowired constructor(
    private val txStorePkgService: TxStorePkgService
)  : TxOpStorePkgResource{
    override fun updatePackageSha256(userId: String, storeType: StoreTypeEnum?, pageSize: Int?):Result<Boolean> {
        return txStorePkgService.updatePackageSha256(userId, storeType, pageSize)
    }
}