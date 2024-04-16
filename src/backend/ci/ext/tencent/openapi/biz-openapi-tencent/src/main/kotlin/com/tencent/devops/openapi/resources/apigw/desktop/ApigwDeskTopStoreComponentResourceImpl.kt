package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopStoreComponentResource
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopStoreComponentResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopStoreComponentResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwDeskTopStoreComponentResourceImpl::class.java)
    }

    override fun getMainPageComponents(
        userId: String,
        storeType: String,
        page: Int,
        pageSize: Int
    ): Result<List<MarketMainItem>> {
        return client.get(ServiceStoreComponentResource::class).getMainPageComponents(
            userId = userId,
            storeType = storeType,
            page = page,
            pageSize = pageSize
        )
    }

    override fun queryComponents(
        userId: String,
        storeType: String,
        projectCode: String?,
        keyword: String?,
        classifyId: String?,
        labelId: String?,
        score: Int?,
        rdType: RdTypeEnum?,
        recommendFlag: Boolean?,
        updateFlag: Boolean?,
        queryProjectComponentFlag: Boolean,
        sortType: StoreSortTypeEnum?,
        page: Int,
        pageSize: Int
    ): Result<Page<MarketItem>> {
        return client.get(ServiceStoreComponentResource::class).queryComponents(
            userId = userId,
            storeType = storeType,
            projectCode = projectCode,
            keyword = keyword,
            classifyId = classifyId,
            labelId = labelId,
            score = score,
            rdType = rdType,
            recommendFlag = recommendFlag,
            updateFlag = updateFlag,
            queryProjectComponentFlag = queryProjectComponentFlag,
            sortType = sortType,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getComponentDetailInfoById(
        userId: String,
        storeType: String,
        storeId: String
    ): Result<StoreDetailInfo?> {
        return client.get(ServiceStoreComponentResource::class).getComponentDetailInfoById(userId, storeType, storeId)
    }

    override fun installComponent(userId: String, installStoreReq: InstallStoreReq): Result<Boolean> {
        return client.get(ServiceStoreComponentResource::class).installComponent(userId, installStoreReq)
    }

    override fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        return client.get(ServiceStoreComponentResource::class).uninstallComponent(
            userId = userId,
            projectCode = projectCode,
            storeType = storeType,
            storeCode = storeCode,
            unInstallReq = unInstallReq
        )
    }
}
