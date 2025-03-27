package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.service.StorePackageDeployService
import com.tencent.devops.store.pojo.common.StoreReleaseInfo
import org.springframework.stereotype.Service

@Service
class SampleStorePackageDeployServiceImpl : StorePackageDeployService() {
    override fun checkStoreReleaseExtInfo(storeReleaseInfo: StoreReleaseInfo): List<String> {
        return emptyList()
    }
}
