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
 *
 */

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SUBPIPELINE"], description = "服务-流水线调用")
@Path("/service/subpipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSubPipelineResource {

    @ApiOperation("获取子流水线启动参数")
    @GET
    @Path("/pipelines/{pipelineId}/manualStartupInfo")
    fun subpipManualStartupInfo(
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = false, defaultValue = "")
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<SubPipelineStartUpInfo>>

    @ApiOperation("从构建机启动指定项目的子流水线")
    @POST
    @Path("/pipelines/{callPipelineId}/atoms/{atomCode}/startByPipeline")
    fun callOtherProjectPipelineStartup(
        @ApiParam("要启动的流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        callProjectId: String,
        @ApiParam("要启动的流水线ID", required = true)
        @PathParam("callPipelineId")
        callPipelineId: String,
        @ApiParam("插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam(value = "父项目ID", required = true)
        @QueryParam("parentProjectId")
        parentProjectId: String,
        @ApiParam("父流水线ID", required = true)
        @QueryParam("parentPipelineId")
        parentPipelineId: String,
        @ApiParam("构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("插件ID", required = true)
        @QueryParam("taskId")
        taskId: String,
        @ApiParam("运行方式", required = true)
        @QueryParam("runMode")
        runMode: String,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>
    ): Result<ProjectBuildId>
}
