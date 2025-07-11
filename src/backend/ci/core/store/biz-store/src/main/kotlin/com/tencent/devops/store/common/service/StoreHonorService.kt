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
import com.tencent.devops.store.pojo.common.honor.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface StoreHonorService {

    /**
     * 查询荣誉列表
     */
    fun list(
        userId: String,
        keyWords: String?,
        page: Int,
        pageSize: Int
    ): Page<StoreHonorManageInfo>

    /**
     * 批量删除组件荣誉
     */
    fun batchDelete(userId: String, storeHonorRelList: List<StoreHonorRel>): Boolean

    /**
     * 添加组件荣誉信息
     */
    fun add(userId: String, addStoreHonorRequest: AddStoreHonorRequest): Result<Boolean>

    /**
     * 获取组件荣誉
     */
    fun getStoreHonor(userId: String, storeType: StoreTypeEnum, storeCode: String): List<HonorInfo>

    /**
     * 佩戴荣誉
     */
    fun installStoreHonor(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        honorId: String
    ): Boolean

    /**
     * 根据组件列表获取关联荣誉信息
     */
    fun getHonorInfosByStoreCodes(
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Map<String, List<HonorInfo>>
}
