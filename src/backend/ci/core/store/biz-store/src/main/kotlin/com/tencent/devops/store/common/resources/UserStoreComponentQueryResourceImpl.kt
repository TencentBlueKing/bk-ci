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
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserStoreComponentQueryResource
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.common.service.StoreMediaService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import com.tencent.devops.store.pojo.common.test.StoreTestItem
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStoreComponentQueryResourceImpl @Autowired constructor(
    private val storeComponentQueryService: StoreComponentQueryService,
    private val storeProjectService: StoreProjectService,
    private val storeMediaService: StoreMediaService
) : UserStoreComponentQueryResource {

    override fun getMyComponents(
        userId: String,
        storeType: String,
        name: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyStoreComponent>?> {
        return Result(
            storeComponentQueryService.getMyComponents(
                userId = userId,
                storeType = storeType,
                name = name,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreDeskVersionItem>> {
        return Result(
            storeComponentQueryService.getComponentVersionsByCode(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                page = page,
                pageSize = pageSize
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

    override fun getComponentDetailInfoByCode(
        userId: String,
        storeType: String,
        storeCode: String
    ): Result<StoreDetailInfo?> {
        return Result(
            storeComponentQueryService.getComponentDetailInfoByCode(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType
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
            ),
            urlProtocolTrim = true
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
                ),
                urlProtocolTrim = true
            )
        )
    }

    override fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): Result<StoreShowVersionInfo> {
        return Result(
            storeComponentQueryService.getComponentShowVersionInfo(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType
            )
        )
    }

    override fun getStoreTestInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<Set<StoreTestItem>> {
        return Result(
            storeProjectService.getStoreTestInfo(userId = userId, storeType = storeType, storeCode = storeCode)
        )
    }

    override fun getStoreMediaInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<List<StoreMediaInfo>?> {
        return storeMediaService.getByCode(storeCode, storeType)
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
                storeCode = storeCode,
                storeType = storeType,
                projectCode = projectCode,
                instanceId = instanceId,
                osName = osName,
                osArch = osArch
            )
        )
    }
}
