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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.JobQuotaProject
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_JOBS_PROJECT_QUOTA"], description = "Job配额管理")
@Path("/op/jobs/quota")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpJobQuotaProjectResource {

    @ApiOperation("获取全部的JOB配额信息")
    @GET
    @Path("/list/project/quota")
    fun list(
        @ApiParam(value = "项目ID", required = false)
        @QueryParam(value = "projectId")
        projectId: String?
    ): Result<List<JobQuotaProject>>

    @ApiOperation("获取项目的JOB配额信息")
    @GET
    @Path("/{projectId}/{vmType}")
    fun get(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType
    ): Result<JobQuotaProject>

    @ApiOperation("添加项目的JOB配额信息")
    @PUT
    @Path("/{projectId}")
    fun add(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "Job配额信息", required = true)
        jobQuota: JobQuotaProject
    ): Result<Boolean>

    @ApiOperation("删除项目的JOB配额信息")
    @DELETE
    @Path("/{projectId}/{vmType}")
    fun delete(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType
    ): Result<Boolean>

    @ApiOperation("更新项目的JOB配额信息")
    @POST
    @Path("/{projectId}/{vmType}")
    fun update(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType,
        @ApiParam(value = "Job配额信息", required = true)
        jobQuota: JobQuotaProject
    ): Result<Boolean>

    @ApiOperation("清零项目的当月已运行时间")
    @POST
    @Path("/project/{projectId}/vm/{vmType}")
    fun restore(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType
    ): Result<Boolean>
}
