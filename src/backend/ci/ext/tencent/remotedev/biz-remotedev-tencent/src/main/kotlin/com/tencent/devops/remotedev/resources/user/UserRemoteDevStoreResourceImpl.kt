package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserRemoteDevStoreResource
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.VersionInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRemoteDevStoreResourceImpl @Autowired constructor(private val client: Client) :
    UserRemoteDevStoreResource {

    override fun getStoreUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String,
        instanceId: String?,
        osName: String?,
        osArch: String?
    ): Result<VersionInfo?> {
        return client.get(ServiceStoreComponentResource::class).getStoreUpgradeVersionInfo(
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            projectCode = projectCode,
            instanceId = instanceId,
            osName = osName,
            osArch = osArch
        )
    }

    override fun installComponent(userId: String, installStoreReq: InstallStoreReq): Result<Boolean> {
        return client.get(ServiceStoreComponentResource::class).installComponent(
            userId = userId,
            installStoreReq = installStoreReq
        )
    }

    override fun getComponentPkgDownloadUrl(
        userId: String,
        projectId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        instanceId: String?,
        osName: String?,
        osArch: String?
    ): Result<String> {
        return client.get(ServiceArchiveComponentPkgResource::class).getComponentPkgDownloadUrl(
            userId = userId,
            projectId = projectId,
            storeType = storeType,
            storeCode = storeCode,
            version = version,
            instanceId = instanceId,
            osName = osName,
            osArch = osArch
        )
    }
}
