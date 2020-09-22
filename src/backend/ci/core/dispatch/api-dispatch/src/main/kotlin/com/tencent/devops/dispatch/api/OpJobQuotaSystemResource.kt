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
import com.tencent.devops.dispatch.pojo.JobQuotaSystem
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
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_JOBS_SYSTEM_QUOTA"], description = "Job默认配额管理")
@Path("/op/jobs/system/quota")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpJobQuotaSystemResource {

    @ApiOperation("获取全部的JOB配额信息")
    @GET
    @Path("/all/list")
    fun list(): Result<List<JobQuotaSystem>>

    @ApiOperation("获取系统默认JOB配额信息")
    @GET
    @Path("/{jobQuotaVmType}")
    fun get(
        @ApiParam(value = "构建机类型", required = false)
        @PathParam("jobQuotaVmType")
        jobQuotaVmType: JobQuotaVmType
    ): Result<List<JobQuotaSystem>>

    @ApiOperation("添加系统默认JOB配额信息")
    @PUT
    @Path("/")
    fun add(
        @ApiParam(value = "Job配额信息", required = true)
        jobQuota: JobQuotaSystem
    ): Result<Boolean>

    @ApiOperation("删除系统默认JOB配额信息")
    @DELETE
    @Path("/{jobQuotaVmType}")
    fun delete(
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("jobQuotaVmType")
        jobQuotaVmType: JobQuotaVmType
    ): Result<Boolean>

    @ApiOperation("更新系统的JOB配额信息")
    @POST
    @Path("/{jobQuotaVmType}")
    fun update(
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("jobQuotaVmType")
        jobQuotaVmType: JobQuotaVmType,
        @ApiParam(value = "Job配额信息", required = true)
        jobQuota: JobQuotaSystem
    ): Result<Boolean>

    @ApiOperation("清零当月已运行时间")
    @POST
    @Path("/clear/vm/{vmType}")
    fun restore(
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType
    ): Result<Boolean>
}
