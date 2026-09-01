package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopStoreComponentVersionResource
import com.tencent.devops.store.api.common.ServiceStoreComponentVersionResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreComponentVersionItem
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopStoreComponentVersionResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopStoreComponentVersionResource {

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int,
        availableFlag: Boolean?
    ): Result<Page<StoreComponentVersionItem>> {
        return client.get(ServiceStoreComponentVersionResource::class).getComponentVersionsByCode(
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            page = page,
            pageSize = pageSize,
            availableFlag = availableFlag
        )
    }

    override fun getComponentPkgDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectCode: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String?,
        osArch: String?
    ): Result<String> {
        return client.get(ServiceArchiveComponentPkgResource::class).getComponentPkgDownloadUrl(
            userId = userId,
            projectId = projectCode,
            storeType = storeType,
            storeCode = storeCode,
            version = version,
            osName = osName,
            osArch = osArch
        )
    }
}
