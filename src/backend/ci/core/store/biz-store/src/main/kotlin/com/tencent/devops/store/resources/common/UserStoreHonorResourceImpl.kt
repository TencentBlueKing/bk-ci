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

package com.tencent.devops.store.resources.common

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserStoreHonorResource
import com.tencent.devops.store.pojo.common.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.HonorInfo
import com.tencent.devops.store.pojo.common.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.StoreHonorRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreHonorService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStoreHonorResourceImpl @Autowired constructor(
    private val storeHonorService: StoreHonorService
) : UserStoreHonorResource {
    override fun list(
        userId: String,
        keyWords: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreHonorManageInfo>> {
        return Result(
            storeHonorService.list(
                userId = userId,
                keyWords = keyWords,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun batchDelete(userId: String, storeHonorRelList: List<StoreHonorRel>): Result<Boolean> {
        return Result(storeHonorService.batchDelete(userId, storeHonorRelList))
    }

    override fun add(userId: String, addStoreHonorRequest: AddStoreHonorRequest): Result<Boolean> {
        return storeHonorService.add(userId, addStoreHonorRequest)
    }

    override fun getStoreHonor(userId: String, storeType: StoreTypeEnum, storeCode: String): List<HonorInfo> {
        return storeHonorService.getStoreHonor(userId, storeType, storeCode)
    }

    override fun installStoreHonor(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        honorId: String
    ): Result<Boolean> {
        return Result(storeHonorService.installStoreHonor(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType,
            honorId = honorId
        ))
    }
}
