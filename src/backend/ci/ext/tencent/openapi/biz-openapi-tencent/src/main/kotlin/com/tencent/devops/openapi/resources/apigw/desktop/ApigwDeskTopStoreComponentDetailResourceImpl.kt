package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopStoreComponentDetailResource
import com.tencent.devops.store.api.common.ServiceStoreComponentBaseResource
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopStoreComponentDetailResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopStoreComponentDetailResource {

    override fun getComponentDetailInfoById(
        appCode: String?,
        apigwType: String?,
        userId: String,
        storeType: String,
        storeId: String
    ): Result<StoreDetailInfo?> {
        return client.get(ServiceStoreComponentBaseResource::class)
            .getComponentDetailInfoById(userId, storeType, storeId)
    }

    override fun getStoreMediaInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<List<StoreMediaInfo>?> {
        return client.get(ServiceStoreComponentBaseResource::class).getStoreMediaInfo(userId, storeType, storeCode)
    }
}
