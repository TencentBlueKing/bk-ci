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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.DynamicParameterInfo
import com.tencent.devops.stream.pojo.ManualTriggerInfo
import com.tencent.devops.stream.pojo.ManualTriggerReq
import com.tencent.devops.stream.pojo.StreamGitYamlString
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.V2BuildYaml
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

@Api(tags = ["USER_STREAM_TRIGGER"], description = "user-TriggerBuild页面")
@Path("/user/trigger/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStreamTriggerResource {

    @ApiOperation("人工TriggerBuild启动构建")
    @POST
    @Path("/{pipelineId}/startup")
    fun triggerStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("TriggerBuild请求", required = true)
        triggerBuildReq: ManualTriggerReq
    ): Result<TriggerBuildResult>

    @ApiOperation("人工TriggerBuild拿启动信息")
    @GET
    @Path("/{projectId}/{pipelineId}/manual")
    fun getManualTriggerInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("分支名称", required = false)
        @QueryParam("branchName")
        branchName: String,
        @ApiParam("COMMIT_ID", required = false)
        @QueryParam("commitId")
        commitId: String?
    ): Result<ManualTriggerInfo>

    @ApiOperation("子流水线插件TriggerBuild拿启动信息")
    @GET
    @Path("/{projectId}/{pipelineId}/manualStartupInfo")
    fun getManualStartupInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("分支名称", required = false)
        @QueryParam("branchName")
        branchName: String,
        @ApiParam("COMMIT_ID", required = false)
        @QueryParam("commitId")
        commitId: String?
    ): Result<List<DynamicParameterInfo>>

    @ApiOperation("校验yaml格式")
    @POST
    @Path("/checkYaml")
    fun checkYaml(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("yaml内容", required = true)
        yaml: StreamGitYamlString
    ): Result<String>

    @ApiOperation("根据BuildId查询yaml内容")
    @GET
    @Path("/getYaml/{projectId}/{buildId}")
    fun getYamlByBuildId(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<V2BuildYaml?>

    @Deprecated("手动触发换新的接口拿取，后续看网关没有调用直接删除")
    @ApiOperation("根据PipelinId和分支查询Yaml内容")
    @GET
    @Path("/{projectId}/{pipelineId}/yaml")
    fun getYamlByPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("分支名称", required = false)
        @QueryParam("branchName")
        branchName: String,
        @ApiParam("COMMIT_ID", required = false)
        @QueryParam("commitId")
        commitId: String?
    ): Result<String?>
}
