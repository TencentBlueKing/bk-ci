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

package com.tencent.devops.engine.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["ENGINE_BUILD"], description = "引擎-构建机请求")
@Path("/build/worker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface EngineBuildResource {
    @ApiOperation("构建机器启动成功")
    @PUT
    @Path("/started")
    fun setStarted(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @ApiParam(value = "网络问题导致的重试次数", required = false)
        @QueryParam("retryCount")
        retryCount: String
    ): Result<BuildVariables>

    @ApiOperation("构建机请求任务")
    @GET
    @Path("/claim")
    fun claimTask(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String
    ): Result<BuildTask>

    @ApiOperation("构建机完成任务")
    @POST
    @Path("/complete")
    fun completeTask(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @ApiParam(value = "执行结果", required = true)
        result: BuildTaskResult
    ): Result<Boolean>

    @ApiOperation("End the seq build")
    @POST
    @Path("/end")
    fun endTask(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String
    ): Result<Boolean>

    @ApiOperation("timeout & end the seq build")
    @POST
    @Path("/timeout")
    fun timeoutTheBuild(
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String
    ): Result<Boolean>

    @ApiOperation("Heartbeat")
    @POST
    @Path("/heartbeat")
    fun heartbeat(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String
    ): Result<Boolean>
}
