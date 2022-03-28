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

package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.op.OpResourceResource
import com.tencent.devops.auth.pojo.enum.SystemType
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.ResourceInfo
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OpResourceResourceImpl @Autowired constructor(

): OpResourceResource {
    override fun createSystemResource(userId: String, resourceInfo: CreateResourceDTO): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun updateSystemResource(userId: String, resourceId: String, resourceInfo: UpdateResourceDTO): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun getSystemResource(resourceId: String): Result<ResourceInfo> {
        TODO("Not yet implemented")
    }

    override fun getSystemResourceByResourceName(resourceName: String): Result<List<ResourceInfo>> {
        TODO("Not yet implemented")
    }

    override fun getSystemResourceBySystem(systemId: SystemType): Result<ResourceInfo> {
        TODO("Not yet implemented")
    }

    override fun listSystemResource(): Result<List<ResourceInfo>> {
        TODO("Not yet implemented")
    }
}