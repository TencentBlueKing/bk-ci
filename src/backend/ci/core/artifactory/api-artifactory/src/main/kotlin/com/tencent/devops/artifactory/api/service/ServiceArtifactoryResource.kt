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

package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.ArtifactoryCreateInfo
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
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

@Api(tags = ["SERVICE_ARTIFACTORY_BASIC"], description = "仓库-文件管理")
@Path("/service/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceArtifactoryResource {

    @ApiOperation("OpenAPI调用创建用户下载链接（持久）")
    @Path("/projects/{projectId}/{artifactoryType}/downloadUrl")
    @POST
    fun downloadUrlForOpenApi(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>

    @ApiOperation("检测文件是否存在")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/check")
    @Path("/{projectId}/{artifactoryType}/check")
    @GET
    fun check(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Boolean>

    @ApiOperation("夸项目拷贝文件")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/acrossProjectCopy")
    @Path("/{projectId}/{artifactoryType}/acrossProjectCopy")
    @POST
    fun acrossProjectCopy(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
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

    @ApiOperation("检测文件是否存在")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/properties")
    @Path("/{projectId}/{artifactoryType}/properties")
    @GET
    fun properties(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<List<Property>>

    @ApiOperation("外部下载链接")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/externalUrl")
    @Path("/{projectId}/{artifactoryType}/externalUrl")
    @GET
    fun externalUrl(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("创建用户", required = false)
        @QueryParam("creatorId")
        creatorId: String?,
        @ApiParam("下载用户", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("有效时间(s)", required = true)
        @QueryParam("ttl")
        ttl: Int,
        @ApiParam("是否直接对应下载链接(false情况下ipa会换成plist下载链接)", required = false)
        @QueryParam("directed")
        directed: Boolean?
    ): Result<Url>

    @ApiOperation("创建内部链接")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/downloadUrl")
    @Path("/{projectId}/{artifactoryType}/downloadUrl")
    @GET
    fun downloadUrl(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("下载用户", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("有效时间(s)", required = true)
        @QueryParam("ttl")
        ttl: Int,
        @ApiParam("是否直接对应下载链接(false情况下ipa会换成plist下载链接)", required = false)
        @QueryParam("directed")
        directed: Boolean?
    ): Result<Url>

    @ApiOperation("获取文件信息")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/show")
    @Path("/{projectId}/{artifactoryType}/show")
    @GET
    fun show(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<FileDetail>

    @ApiOperation("根据元数据获取文件(有排序),searchProps条件为and")
    // @Path("/projects/{projectId}/search")
    @Path("/{projectId}/search")
    @POST
    fun search(
        @ApiParam("用户ID", required = false)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条(不传默认全部返回)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("元数据", required = true)
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>>

    @ApiOperation("根据元数据获取文件和元数据(无排序)")
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/searchFile")
    @Path("/{projectId}/{pipelineId}/{buildId}/searchFile")
    @POST
    fun searchFile(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("通配路径", required = false, defaultValue = "1")
        @QueryParam("regexPath")
        regexPath: String,
        @ApiParam("是否自定义", required = false, defaultValue = "1")
        @QueryParam("customized")
        customized: Boolean,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条(不传默认全部返回)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>>

    @ApiOperation("根据元数据获取文件和元数据(无排序),searchProps条件为and")
    // @Path("/projects/{projectId}/searchFileAndPropertyByAnd")
    @Path("/{projectId}/searchFileAndPropertyByAnd")
    @POST
    fun searchFileAndPropertyByAnd(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条(不传默认全部返回)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("元数据", required = true)
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>>

    @ApiOperation("根据元数据获取文件和元数据(无排序),searchProps条件为or")
    // @Path("/projects/{projectId}/searchFileAndPropertyByOr")
    @Path("/{projectId}/searchFileAndPropertyByOr")
    @POST
    fun searchFileAndPropertyByOr(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条(不传默认全部返回)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("元数据", required = true)
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>>

    @ApiOperation("根据projectId创建临时的Docker仓库用户名密码")
    // @Path("/projects/{projectId}/createDockerUser")
    @Path("/{projectId}/createDockerUser")
    @GET
    fun createDockerUser(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<DockerUser>

    @ApiOperation("设置镜像元数据")
    // @Path("/projects/{projectId}/properties")
    @Path("/{projectId}/properties")
    @POST
    fun setProperties(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("镜像名称", required = true)
        @QueryParam("imageName")
        imageName: String,
        @ApiParam("TAG", required = true)
        @QueryParam("tag")
        tag: String,
        @ApiParam("元数据", required = true)
        properties: Map<String, String>
    ): Result<Boolean>

    @ApiOperation("获取匹配到的自定义仓库文件")
    // @Path("/projects/{projectId}/searchCustomFiles")
    @Path("/{projectId}/searchCustomFiles")
    @POST
    fun searchCustomFiles(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("查询条件", required = true)
        condition: CustomFileSearchCondition
    ): Result<List<String>>

    @ApiOperation("获取时间段内的Jforg信息")
    @Path("/Jforg/info/list")
    @POST
    fun getJforgInfoByteewTime(
        @ApiParam("startTime", required = true)
        @QueryParam("startTime")
        startTime: Long,
        @ApiParam("endTime", required = true)
        @QueryParam("endTime")
        endTime: Long,
        @ApiParam("page", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<List<FileInfo>>

    @ApiOperation("添加APK,IPA产物记录")
    @Path("/create/artifactory/info")
    @POST
    fun createArtifactoryInfo(
        @ApiParam("构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("构建号", required = true)
        @QueryParam("buildNum")
        buildNum: Int,
        @ApiParam("文件信息", required = true)
        fileInfo: FileInfo,
        @ApiParam("数据来源", required = true)
        @QueryParam("dataFrom")
        dataFrom: Int
    ): Result<Long>

    @ApiOperation("批量添加ArtifactoryInfo")
    @Path("/batch/create/artifactoryInfo")
    @POST
    fun batchCreateArtifactoryInfo(
        @ApiParam("infoList", required = true)
        infoList: List<ArtifactoryCreateInfo>
    ): Result<Int>

    @ApiOperation("获取自定义报告根目录Url")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/report/root")
    @GET
    fun getReportRootUrl(
        @ApiParam("项目的英文名", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<String>

    @ApiOperation("根据元数据获取文件")
    @Path("/projects/{projectId}/search")
    @POST
    fun searchFile(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条(不传默认全部返回)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("元数据", required = true)
        searchProps: SearchProps
    ): Result<Page<FileInfo>>
}
