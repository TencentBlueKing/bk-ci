package com.tencent.devops.store.image.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.common.service.StoreLogicExtendService
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import org.springframework.stereotype.Service

@Service("IMAGE_LOGIC_EXTEND_SERVICE")
class ImageLogicExtendServiceImpl : StoreLogicExtendService {

    override fun validateInstallExt(
        userId: String,
        storeCode: String,
        projectCodeList: ArrayList<String>
    ): Result<Boolean> {
        return Result(true)
    }

    override fun installComponentExt(
        userId: String,
        projectCodeList: ArrayList<String>,
        storeBaseDataPO: StoreBaseDataPO,
        storeBaseFeatureDataPO: StoreBaseFeatureDataPO?
    ): Result<Boolean> {
        return Result(true)
    }
}