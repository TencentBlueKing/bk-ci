package com.tencent.devops.store.devx.service

import com.tencent.devops.store.common.service.StorePackageDeployService
import com.tencent.devops.store.pojo.common.StoreReleaseInfo
import com.tencent.devops.store.pojo.devx.constants.KEY_NET_POLICY_INFO
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class DevxPackageDeployService : StorePackageDeployService() {
    override fun checkStoreReleaseExtInfo(storeReleaseInfo: StoreReleaseInfo): List<String> {
        val voidFields = mutableListOf<String>()
        val extBaseInfo = storeReleaseInfo.baseInfo.extBaseInfo
        if (extBaseInfo?.containsKey(KEY_NET_POLICY_INFO) == false) {
            voidFields.add("extBaseInfo.$KEY_NET_POLICY_INFO")
        }
        val baseFeatureInfo = storeReleaseInfo.baseInfo.baseFeatureInfo
        baseFeatureInfo?.let {
            baseFeatureInfo.type ?: voidFields.add("baseFeatureInfo.type")
        }
        return voidFields
    }
}
