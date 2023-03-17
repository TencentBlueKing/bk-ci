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

package com.tencent.devops.dispatch.devcloud.api.builds

import com.tencent.devops.dispatch.devcloud.pojo.devcloud.JobResponse
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.DevCloudJobReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_DEVCLOUD"], description = "构建-devcloud资源操作")
@Path("/build/devcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildDevCloudResource {

    @ApiOperation("启动job")
    @POST
    @Path("/job")
    fun createJob(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("Job结构", required = true)
        jobReq: DevCloudJobReq
    ): Result<JobResponse>

    @ApiOperation("获取job状态")
    @GET
    @Path("/job/{jobName}/status")
    fun getJobStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("jobName", required = true)
        @PathParam("jobName")
        jobName: String
    ): Result<String>

    @ApiOperation("获取job状态")
    @GET
    @Path("/job/{jobName}/logs")
    fun getJobLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("jobName", required = true)
        @PathParam("jobName")
        jobName: String
    ): Result<String>

    @ApiOperation("获取job状态")
    @GET
    @Path("/task/{taskId}")
    fun getTask(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("taskId", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<String>
}
