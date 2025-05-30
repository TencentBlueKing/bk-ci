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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.QueryComponentsParam
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo

interface StoreComponentQueryService {

    /**
     * 根据用户获取工作台组件列表
     */
    fun getMyComponents(
        userId: String,
        storeType: String,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<MyStoreComponent>?

    /**
     * 查询所有流程中组件信息
     */
    fun listComponents(
        userId: String,
        queryComponentsParam: QueryComponentsParam
    ): Page<MyStoreComponent>?

    /**
     * 根据组件标识获取组件版本列表
     */
    @Suppress("LongParameterList")
    fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int,
        checkPermissionFlag: Boolean = true
    ): Page<StoreDeskVersionItem>

    /**
     * 根据组件ID获取组件详情
     */
    fun getComponentDetailInfoById(
        userId: String,
        storeType: StoreTypeEnum,
        storeId: String
    ): StoreDetailInfo?

    /**
     * 根据组件代码获取组件详情
     */
    fun getComponentDetailInfoByCode(
        userId: String,
        storeType: String,
        storeCode: String
    ): StoreDetailInfo?

    /**
     * 获取研发商店首页组件的数据
     */
    fun getMainPageComponents(
        userId: String,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean = false
    ): Result<List<MarketMainItem>>

    /**
     * 根据条件查询组件列表
     */
    fun queryComponents(
        userId: String,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean = false
    ): Page<MarketItem>

    /**
     * 根据组件标识获取组件回显版本信息
     */
    fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): StoreShowVersionInfo

    /**
     * 获取组件升级版本信息
     */
    fun getComponentUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String = "",
        instanceId: String? = null,
        osName: String? = null,
        osArch: String? = null
    ): VersionInfo?
}
