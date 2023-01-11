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

package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.index.CreateIndexComputeDetailRequest
import com.tencent.devops.store.pojo.common.index.StoreIndexBaseInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexCreateRequest
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo

interface StoreIndexManageService {

    fun add(userId: String, storeIndexCreateRequest: StoreIndexCreateRequest): Result<Boolean>

    fun delete(userId: String, indexId: String): Result<Boolean>

    fun list(userId: String, keyWords: String?, page: Int, pageSize: Int): Page<StoreIndexBaseInfo>

    fun getStoreIndexInfosByStoreCodes(
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Map<String, List<StoreIndexInfo>>

    fun getStoreIndexInfosByStoreCode(
        storeType: StoreTypeEnum,
        storeCode: String
    ): List<StoreIndexInfo>

    fun createIndexComputeDetail(
        userId: String,
        createIndexComputeDetailRequest: CreateIndexComputeDetailRequest
    ): Result<Boolean>

    fun getStoreCodeByElementValue(indexCode: String, elementName: String): Result<List<String>>

    fun deleteStoreIndexResultByStoreCode(
        userId: String,
        indexCode: String,
        storeCodes: List<String>
    ): Result<Boolean>
}
