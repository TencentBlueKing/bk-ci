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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.Permission
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineRemoteToken
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.PipelineStageTag
import com.tencent.devops.process.pojo.PipelineStatus
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewAndPipelines
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.BatchDeletePipeline
import com.tencent.devops.process.pojo.pipeline.PipelineCount
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["USER_PIPELINE"], description = "用户-流水线资源")
@Path("/user/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPipelineResource {

    @ApiOperation("用户是否拥有创建流水线权限")
    @GET
    // @Path("/projects/{projectId}/hasCreatePermission")
    @Path("/{projectId}/hasCreatePermission")
    fun hasCreatePermission(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("流水线名是否存在")
    @GET
    // @Path("/projects/{projectId}/pipelineExist")
    @Path("/{projectId}/pipelineExist")
    fun pipelineExist(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线名", required = true)
        @QueryParam("pipelineName")
        pipelineName: String
    ): Result<Boolean>

    @ApiOperation("拥有权限流水线列表")
    @GET
    // @Path("/projects/{projectId}/hasPermissionList")
    @Path("/{projectId}/hasPermissionList")
    fun hasPermissionList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("对应权限", required = true, defaultValue = "")
        @QueryParam("permission")
        permission: Permission,
        @ApiParam("排除流水线ID", required = false, defaultValue = "")
        @QueryParam("excludePipelineId")
        excludePipelineId: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<Pipeline>>

    @ApiOperation("新建流水线编排")
    @POST
    // @Path("/projects/{projectId}/createPipeline")
    @Path("/{projectId}")
    fun create(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false,
        @ApiParam(value = "流水线模型", required = true)
        pipeline: Model
    ): Result<PipelineId>

    @ApiOperation("用户是否拥有流水线权限")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/hasPermission")
    @Path("/{projectId}/{pipelineId}/hasPermission")
    fun hasPermission(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线模型", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "权限", required = true)
        @QueryParam("permission")
        permission: Permission
    ): Result<Boolean>

    @ApiOperation("复制流水线编排")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/copy")
    @Path("/{projectId}/{pipelineId}/copy")
    fun copy(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线模型", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线COPY", required = true)
        pipeline: PipelineCopy
    ): Result<PipelineId>

    @ApiOperation("编辑流水线编排")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/")
    @Path("/{projectId}/{pipelineId}/")
    fun edit(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线模型", required = true)
        pipeline: Model
    ): Result<Boolean>

    @ApiOperation("编辑流水线编排以及设置")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/saveAll")
    @Path("/{projectId}/{pipelineId}/saveAll")
    fun saveAll(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线模型与设置", required = true)
        @Valid
        modelAndSetting: PipelineModelAndSetting
    ): Result<Boolean>

    @ApiOperation("保存流水线设置")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/saveSetting")
    @Path("/{projectId}/{pipelineId}/saveSetting")
    fun saveSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>

    @ApiOperation("获取流水线编排")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/")
    @Path("/{projectId}/{pipelineId}/")
    fun get(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Model>

    @ApiOperation("获取流水线编排版本")
    @GET
    @Path("/{projectId}/{pipelineId}/{version}")
    fun getVersion(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线编排版本", required = true)
        @PathParam("version")
        version: Int
    ): Result<Model>

    @ApiOperation("生成远程执行token")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/remoteToken")
    @Path("/{projectId}/{pipelineId}/remoteToken")
    fun generateRemoteToken(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineRemoteToken>

    @ApiOperation("删除流水线编排")
    @DELETE
    @Path("/{projectId}/{pipelineId}/")
    fun softDelete(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @ApiOperation("批量删除流水线编排")
    @DELETE
    @Path("/batchDelete")
    fun batchDelete(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        batchDeletePipeline: BatchDeletePipeline
    ): Result<Map<String, Boolean>>

    @ApiOperation("删除流水线版本")
    @DELETE
    @Path("/{projectId}/{pipelineId}/{version}/")
    fun deleteVersion(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线编排版本", required = true)
        @PathParam("version")
        version: Int
    ): Result<Boolean>

    @ApiOperation("用户获取视图设置和流水线编排列表")
    @GET
    // @Path("/projects/{projectId}/listViewSettingAndPipelines")
    @Path("/projects/{projectId}/listViewSettingAndPipelines")
    fun listViewSettingAndPipelines(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<PipelineViewAndPipelines>

    @ApiOperation("用户获取视图流水线编排列表")
    @GET
    @Path("/projects/{projectId}/listViewPipelines")
    fun listViewPipelines(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @ApiParam("按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?,
        @ApiParam("按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String?,
        @ApiParam("按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String?,
        @ApiParam("按视图过滤", required = false)
        @QueryParam("filterByViewIds")
        filterByViewIds: String? = null,
        @ApiParam("用户视图ID,表示用户当前所在视图", required = true)
        @QueryParam("viewId")
        viewId: String,
        @ApiParam("排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation?,
        @ApiParam("是否展示已删除流水线", required = false)
        @QueryParam("showDelete")
        showDelete: Boolean? = false
    ): Result<PipelineViewPipelinePage<Pipeline>>

    @ApiOperation("有权限流水线编排列表")
    @GET
    @Path("/{projectId}/")
    fun list(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME
    ): Result<PipelinePage<Pipeline>>

    @ApiOperation("流水线状态列表")
    @POST
    // @Path("/projects/{projectId}/pipelineStatus")
    @Path("/{projectId}/pipelineStatus")
    fun getPipelineStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Pipeline ID 列表", required = true)
        pipelines: Set<String>
    ): Result<Map<String, PipelineStatus>>

    @ApiOperation("收藏流水线消息")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/favor")
    @Path("/{projectId}/{pipelineId}/favor")
    fun favor(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("是否收藏", required = true)
        @QueryParam("type")
        favor: Boolean
    ): Result<Boolean>

    @ApiOperation("还原流水线编排")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/restore")
    @Path("/{projectId}/{pipelineId}/restore")
    fun restore(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @ApiOperation("列出等还原回收的流水线列表")
    @GET
    @Path("/{projectId}/pipelineRecycleList")
    fun recycleList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @ApiParam("排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation?
    ): Result<PipelineViewPipelinePage<PipelineInfo>>

    @ApiOperation("流水线重命名")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/rename")
    @Path("/{projectId}/{pipelineId}/")
    fun rename(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线名称", required = true)
        name: PipelineName
    ): Result<Boolean>

    @ApiOperation("获取流水线阶段标签")
    @GET
    @Path("/stageTag")
    fun getStageTag(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<PipelineStageTag>>

    @ApiOperation("导出流水线模板")
    @GET
    @Path("{pipelineId}/projects/{projectId}/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Response

    @ApiOperation("导入流水线模板")
    @POST
    @Path("/projects/{projectId}/upload")
    fun uploadPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目Id", required = true)
        pipelineInfo: PipelineModelAndSetting,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<String?>

    @ApiOperation("流水线编排版本列表")
    @GET
    @Path("/{projectId}/{pipelineId}/version")
    fun versionList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<PipelineViewPipelinePage<PipelineInfo>>

    @ApiOperation("校验matrix yaml格式")
    @POST
    @Path("/{projectId}/{pipelineId}/matrix/check")
    fun checkYaml(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("yaml内容", required = true)
        yaml: MatrixPipelineInfo
    ): Result<MatrixPipelineInfo>

    @ApiOperation("获取列表页列表相关的数目")
    @GET
    @Path("/projects/{projectId}/getCount")
    fun getCount(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineCount>
}
