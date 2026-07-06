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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.QueryGroupParam
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import org.jooq.Record

/**
 * 组件市场查询服务(按业务维度从 StoreComponentQueryService 拆分)。
 */
interface StoreComponentMarketQueryService {

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
     * 将给定的组件记录富化为市场组件项(MarketItem)。
     * 供需要复用市场富化逻辑(安装标识、Logo、统计、国际化等)、但自行控制取数与分页的场景使用(如部署信息聚合)。
     */
    fun enrichMarketItems(
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        storeInfos: List<Record>,
        urlProtocolTrim: Boolean = false
    ): List<MarketItem>

    /**
     * 统计分组信息
     */
    fun getComponentGroupCount(
        userId: String,
        queryGroupParam: QueryGroupParam
    ): List<Pair<String, Int>>
}
