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

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.stream.pojo.StreamModelDetail
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

@Api(tags = ["USER_STREAM_CURRENT"], description = "user-BuildDetail页面")
@Path("/user/current/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStreamDetailResource {

    @ApiOperation("查看指定的构建详情")
    @GET
    @Path("/detail/{projectId}")
    fun getLatestBuildDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam(value = "构建ID", required = false)
        @QueryParam("buildId")
        buildId: String?
    ): Result<StreamModelDetail?>

    @ApiOperation("触发审核")
    @POST
    @Path("/detail/{projectId}/review")
    fun buildTriggerReview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "构建ID", required = false)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("是否通过审核", required = true)
        @QueryParam("approve")
        approve: Boolean
    ): Result<Boolean>

    @ApiOperation("根据元数据获取文件(有排序),searchProps条件为and")
    @Path("/artifactories/projects/{projectId}/search")
    @GET
    fun search(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条(不传默认全部返回)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>>

    @ApiOperation("创建下载链接")
    @Path("/artifactories/projects/{projectId}/artifactoryType/{artifactoryType}/downloadUrl")
    @POST
    fun downloadUrl(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("stream 用户ID", required = true, defaultValue = "0")
        @HeaderParam("X-GIT-UID")
        gitUserId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>

    @ApiOperation("获取构建报告列表")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/report")
    @GET
    fun getReports(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<Report>>
}
