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
import com.tencent.devops.dispatch.pojo.JobQuotaStatus
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_JOBS_PROJECT_QUOTA"], description = "Job配额管理")
@Path("/service/jobs/running")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceJobQuotaBusinessResource {

    @ApiOperation("上报一个JOB启动")
    @POST
    @Path("/job/{projectId}/{vmType}/{buildId}/{vmSeqId}")
    fun addRunningJob(
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "vmType", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>

    @ApiOperation("上报一个JOB结束")
    @DELETE
    @Path("/job/{projectId}/{buildId}/{vmSeqId}")
    fun removeRunningJob(
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "vmSeqId", required = false)
        @PathParam("vmSeqId")
        vmSeqId: String?
    ): Result<Boolean>

    @ApiOperation("上报一个Agent启动")
    @POST
    @Path("/agent/{projectId}/{buildId}/{vmSeqId}")
    fun addRunningAgent(
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>

    @ApiOperation("上报一个Agent结束")
    @DELETE
    @Path("/agent/{projectId}/{buildId}/{vmSeqId}")
    fun removeRunningAgent(
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>

    @ApiOperation("获取项目下正在执行的JOB配额状态")
    @GET
    @Path("/count/{projectId}/{vmType}")
    fun getRunningJobCount(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "构建机类型", required = true)
        @PathParam("vmType")
        vmType: JobQuotaVmType
    ): Result<JobQuotaStatus>
}
