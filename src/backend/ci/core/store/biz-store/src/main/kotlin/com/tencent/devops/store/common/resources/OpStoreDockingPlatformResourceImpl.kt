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

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.OpStoreDockingPlatformResource
import com.tencent.devops.store.pojo.common.platform.StoreDockingPlatformInfo
import com.tencent.devops.store.pojo.common.platform.StoreDockingPlatformRequest
import com.tencent.devops.store.common.service.StoreDockingPlatformService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpStoreDockingPlatformResourceImpl @Autowired constructor(
    private val storeDockingPlatformService: StoreDockingPlatformService
) : OpStoreDockingPlatformResource {

    override fun add(
        userId: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Result<Boolean> {
        return Result(storeDockingPlatformService.create(userId, storeDockingPlatformRequest))
    }

    override fun update(
        userId: String,
        id: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Result<Boolean> {
        return Result(storeDockingPlatformService.update(userId, id, storeDockingPlatformRequest))
    }

    override fun listPlatforms(
        userId: String,
        platformName: String?,
        id: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreDockingPlatformInfo>?> {
        return Result(
            storeDockingPlatformService.getStoreDockingPlatforms(
                userId = userId,
                platformName = platformName,
                id = id,
                page = page,
                pageSize = pageSize)
        )
    }

    override fun deletePlatformById(userId: String, id: String): Result<Boolean> {
        return Result(storeDockingPlatformService.delete(userId, id))
    }
}
