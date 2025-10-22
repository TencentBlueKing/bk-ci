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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.pojo.JobQuotaProject
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_JOBS_PROJECT_QUOTA", description = "Job配额管理")
@Path("/op/jobs/quota")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpJobQuotaProjectResource {

    @Operation(summary = "获取全部的JOB配额信息")
    @GET
    @Path("/list/project/quota")
    fun list(
        @Parameter(description = "项目ID", required = false)
        @QueryParam(value = "projectId")
        projectId: String?
    ): Result<List<JobQuotaProject>>

    @Operation(summary = "获取项目的JOB配额信息")
    @GET
    @Path("/{projectId}/{vmType}")
    fun get(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType,
        @Parameter(description = "构建来源", required = false)
        @QueryParam("channelCode")
        channelCode: String = ChannelCode.BS.name
    ): Result<JobQuotaProject>

    @Operation(summary = "添加项目的JOB配额信息")
    @PUT
    @Path("/{projectId}")
    fun add(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Job配额信息", required = true)
        jobQuota: JobQuotaProject
    ): Result<Boolean>

    @Operation(summary = "删除项目的JOB配额信息")
    @DELETE
    @Path("/{projectId}/{vmType}")
    fun delete(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType,
        @Parameter(description = "构建来源", required = false)
        @QueryParam("channelCode")
        channelCode: String = ChannelCode.BS.name
    ): Result<Boolean>

    @Operation(summary = "更新项目的JOB配额信息")
    @POST
    @Path("/{projectId}/{vmType}")
    fun update(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType,
        @Parameter(description = "Job配额信息", required = true)
        jobQuota: JobQuotaProject
    ): Result<Boolean>

    @Operation(summary = "清零异常的构建配额记录")
    @POST
    @Path("/project/{projectId}/vm/{vmType}")
    fun restoreProjectRunningJobs(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType,
        @Parameter(description = "构建时间", required = true)
        @QueryParam("createTime")
        createTime: String,
        @Parameter(description = "构建来源", required = false)
        @QueryParam("channelCode")
        channelCode: String = ChannelCode.BS.name
    ): Result<Boolean>
}
