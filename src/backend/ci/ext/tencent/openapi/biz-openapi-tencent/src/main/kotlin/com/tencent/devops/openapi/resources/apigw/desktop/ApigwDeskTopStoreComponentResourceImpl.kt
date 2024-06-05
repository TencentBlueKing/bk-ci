package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.DateTimeUtil.YYYY_MM_DD
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopStoreComponentResource
import com.tencent.devops.openapi.api.apigw.pojo.StoreDailyStatisticInfo
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.api.common.ServiceStoreStatisticResource
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatisticRequest
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopStoreComponentResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopStoreComponentResource {

    override fun getMainPageComponents(
        appCode: String?,
        apigwType: String?,
        userId: String,
        storeType: String,
        projectCode: String?,
        instanceId: String?,
        page: Int,
        pageSize: Int
    ): Result<List<MarketMainItem>> {
        return client.get(ServiceStoreComponentResource::class).getMainPageComponents(
            userId = userId,
            storeType = storeType,
            projectCode = projectCode,
            instanceId = instanceId,
            page = page,
            pageSize = pageSize
        )
    }

    override fun queryComponents(
        appCode: String?,
        apigwType: String?,
        userId: String,
        storeType: String,
        projectCode: String?,
        keyword: String?,
        classifyId: String?,
        categoryId: String?,
        labelId: String?,
        score: Int?,
        rdType: RdTypeEnum?,
        recommendFlag: Boolean?,
        installed: Boolean?,
        updateFlag: Boolean?,
        queryProjectComponentFlag: Boolean,
        sortType: StoreSortTypeEnum?,
        instanceId: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MarketItem>> {
        return client.get(ServiceStoreComponentResource::class).queryComponents(
            userId = userId,
            storeType = storeType,
            projectCode = projectCode,
            keyword = keyword,
            classifyId = classifyId,
            categoryId = categoryId,
            labelId = labelId,
            score = score,
            rdType = rdType,
            recommendFlag = recommendFlag,
            installed = installed,
            updateFlag = updateFlag,
            queryProjectComponentFlag = queryProjectComponentFlag,
            sortType = sortType,
            instanceId = instanceId,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getComponentDetailInfoById(
        appCode: String?,
        apigwType: String?,
        userId: String,
        storeType: String,
        storeId: String
    ): Result<StoreDetailInfo?> {
        return client.get(ServiceStoreComponentResource::class).getComponentDetailInfoById(userId, storeType, storeId)
    }

    override fun installComponent(
        appCode: String?,
        apigwType: String?,
        userId: String,
        installStoreReq: InstallStoreReq
    ): Result<Boolean> {
        return client.get(ServiceStoreComponentResource::class).installComponent(userId, installStoreReq)
    }

    override fun uninstallComponent(
        appCode: String?,
        apigwType: String?,
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

    override fun updateDailyStatisticInfo(
        appCode: String?,
        apigwType: String?,
        storeType: StoreTypeEnum,
        storeCode: String,
        storeDailyStatisticInfo: StoreDailyStatisticInfo
    ): Result<Boolean> {
        val storeDailyStatisticRequest = StoreDailyStatisticRequest(
            dailyActiveDuration = storeDailyStatisticInfo.dailyActiveDuration,
            statisticsTime = DateTimeUtil.stringToLocalDateTime(storeDailyStatisticInfo.statisticsTime, YYYY_MM_DD)
        )
        return client.get(ServiceStoreStatisticResource::class).updateDailyStatisticInfo(
            storeType = storeType,
            storeCode = storeCode,
            storeDailyStatisticRequest = storeDailyStatisticRequest
        )
    }
}
