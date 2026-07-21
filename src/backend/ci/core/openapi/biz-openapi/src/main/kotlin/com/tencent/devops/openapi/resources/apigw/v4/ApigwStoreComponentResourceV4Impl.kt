/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwStoreComponentResourceV4
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.api.common.ServiceStoreInstalledComponentResource
import com.tencent.devops.store.pojo.common.InstalledComponentInfo
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStoreComponentResourceV4Impl @Autowired constructor(private val client: Client) :
    ApigwStoreComponentResourceV4 {

    override fun queryComponents(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectCode: String,
        storeType: String,
        keyword: String?,
        classifyId: String?,
        categoryId: String?,
        labelId: String?,
        score: Int?,
        rdType: RdTypeEnum?,
        recommendFlag: Boolean?,
        installed: Boolean?,
        updateFlag: Boolean?,
        sortType: StoreSortTypeEnum?,
        instanceId: String?,
        queryTestFlag: Boolean?,
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
            queryProjectComponentFlag = true,
            sortType = sortType,
            instanceId = instanceId,
            queryTestFlag = queryTestFlag,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getInstalledComponents(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectCode: String,
        storeType: StoreTypeEnum,
        instanceIds: Set<String>?,
        page: Int,
        pageSize: Int
    ): Result<Page<InstalledComponentInfo>> {
        return client.get(ServiceStoreInstalledComponentResource::class).getInstalledComponents(
            userId = userId,
            projectCode = projectCode,
            storeType = storeType,
            instanceIds = instanceIds,
            page = page,
            pageSize = pageSize
        )
    }
}
