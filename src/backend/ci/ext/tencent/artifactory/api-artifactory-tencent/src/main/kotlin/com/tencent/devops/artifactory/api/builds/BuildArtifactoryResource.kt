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

package com.tencent.devops.artifactory.api.builds

import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_REGION
import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["BUILD_ARTIFACTORY"], description = "版本仓库-仓库资源")
@Path("/build/artifactories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildArtifactoryResource {

    @ApiOperation("获取文件元数据")
    @Path("/properties")
    @GET
    fun getProperties(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam(value = "path")
        path: String
    ): Result<List<Property>>

    @ApiOperation("设置文件元数据")
    @Path("/properties")
    @POST
    fun setProperties(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("元数据", required = true)
        properties: Map<String, String>
    ): Result<Boolean>

    @ApiOperation("查询文件元数据")
    @Path("/getPropertiesByRegex")
    @GET
    fun getPropertiesByRegex(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        crossProjectId: String?,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        crossPipineId: String?,
        @ApiParam("构建No", required = false)
        @QueryParam("buildNo")
        crossBuildNo: String?
    ): Result<List<FileDetail>>

    @ApiOperation("获取文件第三方下载链接")
    @Path("/thirdPartyDownloadUrl")
    @GET
    fun getThirdPartyDownloadUrl(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("有效时间(s)", required = true)
        @QueryParam("ttl")
        ttl: Int?,
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        crossProjectId: String?,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        crossPipineId: String?,
        @ApiParam("构建No", required = false)
        @QueryParam("buildNo")
        crossBuildNo: String?,
        @ApiParam("客户端区域", required = false)
        @HeaderParam(AUTH_HEADER_REGION)
        region: String?
    ): Result<List<String>>

    @ApiOperation("获取文件下载url")
    @Path("/project/{projectId}/pipeline/{pipelineId}/buildId/{buildId}/getFileDownloadUrl")
    @GET
    fun getFileDownloadUrl(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<List<String>>

    @ApiOperation("获取我的文件列表")
    @Path("/users/{userId}/ownFileList")
    @GET
    fun getOwnFileList(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String
    ): Result<FileInfoPage<FileInfo>>

    @ApiOperation("检测文件是否存在")
    @Path("/projects/{projectId}/fileCheck")
    @GET
    fun check(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Boolean>

    @ApiOperation("跨项目拷贝文件")
    @Path("/artifactoryType/{artifactoryType}/acrossProjectCopy")
    @GET
    fun acrossProjectCopy(
        @ApiParam("项目ID", required = true)
        @HeaderParam("X-DEVOPS-PROJECT-ID")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("目标项目", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @ApiParam("目标路径", required = true)
        @QueryParam("targetPath")
        targetPath: String
    ): Result<Count>

    @ApiOperation("检查项目是否灰度仓库")
    @Path("/checkRepoGray")
    @GET
    fun checkRepoGray(
        @ApiParam("项目ID", required = true)
        @HeaderParam("X-DEVOPS-PROJECT-ID")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("检查项目是否灰度")
    @Path("/checkGrayProject")
    @GET
    fun checkGrayProject(
        @ApiParam("项目ID", required = true)
        @HeaderParam("X-DEVOPS-PROJECT-ID")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("创建外部下载链接")
    @Path("/{artifactoryType}/externalUrl")
    @GET
    fun externalUrl(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("完整路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>
}
