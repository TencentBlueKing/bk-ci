package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopStoreResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopStoreResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopStoreResource {

    override fun getClassifyList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        storeType: StoreTypeEnum
    ): Result<List<Classify>> {
        return client.get(ServiceStoreResource::class).getClassifyList(storeType)
    }
}
