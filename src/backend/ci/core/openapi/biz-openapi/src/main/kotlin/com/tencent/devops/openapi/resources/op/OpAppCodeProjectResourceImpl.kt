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
package com.tencent.devops.openapi.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.op.OpAppCodeProjectResource
import com.tencent.devops.openapi.pojo.AppCodeProjectResponse
import com.tencent.devops.openapi.service.op.AppCodeProjectService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAppCodeProjectResourceImpl @Autowired constructor(
    private val appCodeProjectService: AppCodeProjectService
) : OpAppCodeProjectResource {
    companion object {
        private val logger = LoggerFactory.getLogger(OpAppCodeProjectResourceImpl::class.java)
    }

    override fun addProject(userName: String, appCode: String, projectId: String): Result<Boolean> {
        return Result(appCodeProjectService.addProject(userName, appCode, projectId))
    }

    override fun listProject(userName: String): Result<List<AppCodeProjectResponse>> {
        return Result(appCodeProjectService.listProject(userName))
    }

    override fun listProjectByAppCode(userName: String, appCode: String): Result<List<AppCodeProjectResponse>> {
        return Result(appCodeProjectService.listProjectByAppCode(appCode))
    }

    override fun getProject(userName: String, appCode: String, projectId: String): Result<AppCodeProjectResponse?> {
        return Result(appCodeProjectService.getProject(userName, appCode, projectId))
    }

    override fun deleteProject(userName: String, appCode: String, projectId: String): Result<Boolean> {
        return Result(appCodeProjectService.deleteProject(userName, appCode, projectId))
    }
}
