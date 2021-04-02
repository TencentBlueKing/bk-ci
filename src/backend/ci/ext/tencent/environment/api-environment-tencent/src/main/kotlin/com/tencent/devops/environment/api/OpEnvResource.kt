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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.ProjectConfig
import com.tencent.devops.environment.pojo.ProjectConfigPage
import com.tencent.devops.environment.pojo.ProjectConfigParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_ENV"], description = "环境管理")
@Path("/op/env")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpEnvResource {
    @ApiOperation("保存项目配置")
    @POST
    @Path("/project/saveProjectConfig")
    fun saveProjectConfig(
        @ApiParam("项目配置", required = true)
        projectConfigParam: ProjectConfigParam
    ): Result<Boolean>

    @ApiOperation("项目配置列表")
    @GET
    @Path("/project/listProjectConfig")
    fun listProjectConfig(): Result<List<ProjectConfig>>

    @ApiOperation("项目配置列表（分页）")
    @GET
    @Path("/projectConfig/list")
    fun list(
        @ApiParam(value = "第几页，从1开始", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam(value = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<ProjectConfigPage>
}
