/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.VersionInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStoreComponentResourceImpl @Autowired constructor(
    private val storeComponentQueryService: StoreComponentQueryService,
    private val storeComponentManageService: StoreComponentManageService
) : ServiceStoreComponentResource {

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
        return storeComponentManageService.uninstallComponent(
            userId = userId,
            projectCode = projectCode,
            storeType = storeType,
            storeCode = storeCode,
            unInstallReq = unInstallReq
        )
    }

    override fun getStoreUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String,
        instanceId: String?,
        osName: String?,
        osArch: String?
    ): Result<VersionInfo?> {
        return Result(
            storeComponentQueryService.getComponentUpgradeVersionInfo(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                projectCode = projectCode,
                instanceId = instanceId,
                osName = osName,
                osArch = osArch
            )
        )
    }

    override fun getComponentDetailInfoById(
        userId: String,
        storeType: String,
        storeId: String
    ): Result<StoreDetailInfo?> {
        return Result(
            storeComponentQueryService.getComponentDetailInfoById(
                userId = userId,
                storeId = storeId,
                storeType = StoreTypeEnum.valueOf(storeType)
            )
        )
    }

    override fun getMainPageComponents(
        userId: String,
        storeType: String,
        projectCode: String?,
        instanceId: String?,
        page: Int,
        pageSize: Int
    ): Result<List<MarketMainItem>> {
        return storeComponentQueryService.getMainPageComponents(
            userId = userId,
            storeInfoQuery = StoreInfoQuery(
                storeType = storeType,
                projectCode = projectCode,
                instanceId = instanceId,
                page = page,
                pageSize = pageSize,
                queryProjectComponentFlag = false
            )
        )
    }

    override fun queryComponents(
        userId: String,
        storeType: String,
        projectCode: String?,
        keyword: String?,
        classifyId: String?,
        labelId: String?,
        categoryId: String?,
        score: Int?,
        rdType: RdTypeEnum?,
        recommendFlag: Boolean?,
        installed: Boolean?,
        updateFlag: Boolean?,
        queryProjectComponentFlag: Boolean,
        sortType: StoreSortTypeEnum?,
        instanceId: String?,
        queryTestFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<Page<MarketItem>> {
        return Result(
            storeComponentQueryService.queryComponents(
                userId = userId,
                storeInfoQuery = StoreInfoQuery(
                    storeType = storeType,
                    projectCode = projectCode,
                    keyword = keyword,
                    classifyId = classifyId,
                    labelId = labelId,
                    categoryId = categoryId,
                    score = score,
                    recommendFlag = recommendFlag,
                    rdType = rdType,
                    queryProjectComponentFlag = queryProjectComponentFlag,
                    sortType = sortType,
                    instanceId = instanceId,
                    queryTestFlag = queryTestFlag,
                    page = page,
                    pageSize = pageSize,
                    installed = installed,
                    updateFlag = updateFlag
                )
            )
        )
    }
}
