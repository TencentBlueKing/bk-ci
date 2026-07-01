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

import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

/**
 * 组件详情查询服务(按业务维度从 StoreComponentQueryService 拆分)。
 */
interface StoreComponentDetailQueryService {

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
        storeCode: String,
        version: String? = null,
        ownerStoreCode: String? = null
    ): StoreDetailInfo?

    /**
     * 根据组件标识获取组件要素信息，不校验用户权限
     */
    fun getComponentDataInfoByCode(
        storeType: String,
        storeCode: String,
        version: String? = null,
        ownerStoreCode: String? = null,
        status: StoreStatusEnum? = null
    ): StoreDetailInfo?
}
