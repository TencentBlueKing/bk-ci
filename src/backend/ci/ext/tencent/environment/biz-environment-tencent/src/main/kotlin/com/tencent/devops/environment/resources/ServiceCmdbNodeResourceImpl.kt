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

package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceCmdbNodeResource
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.service.CmdbNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCmdbNodeResourceImpl @Autowired constructor(
    private val cmdbNodeService: CmdbNodeService
) : ServiceCmdbNodeResource {

    override fun listUserCmdbNodesNew(
        userId: String,
        bakOperator: Boolean,
        page: Int,
        pageSize: Int,
        ips: List<String>?
    ): Result<Page<CmdbNode>> {
        return Result(cmdbNodeService.getUserCmdbNodesNew(
            userId = userId,
            bakOperator = bakOperator,
            page = page,
            pageSize = pageSize,
            ips = ips ?: listOf()
        ))
    }

    override fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>): Result<Boolean> {
        cmdbNodeService.addCmdbNodes(userId = userId, projectId = projectId, nodeIps = nodeIps)
        return Result(true)
    }
}
