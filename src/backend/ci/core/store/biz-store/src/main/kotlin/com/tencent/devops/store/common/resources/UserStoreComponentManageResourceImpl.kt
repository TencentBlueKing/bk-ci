package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.api.common.UserStoreComponentManageResource
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq

class UserStoreComponentManageResourceImpl(
    private val storeComponentManageService: StoreComponentManageService
) : UserStoreComponentManageResource {
    override fun updateComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun installComponent(userId: String, installStoreReq: InstallStoreReq): Result<Boolean> {
        return storeComponentManageService.installComponent(
            userId = userId,
            channelCode = ChannelCode.BS,
            installStoreReq = installStoreReq
        )
    }

    override fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun deleteComponent(userId: String, storeType: String, storeCode: String): Result<Boolean> {
        TODO("Not yet implemented")
    }
}