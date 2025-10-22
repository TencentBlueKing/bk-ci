/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.pojo.Permission
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdAndName
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "USER_PIPELINE", description = "用户-流水线资源")
@Path("/user/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPipelineResource {

    @Operation(summary = "用户是否拥有创建流水线权限")
    @GET
    // @Path("/projects/{projectId}/hasCreatePermission")
    @Path("/{projectId}/hasCreatePermission")
    fun hasCreatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "流水线名是否存在")
    @GET
    // @Path("/projects/{projectId}/pipelineExist")
    @Path("/{projectId}/pipelineExist")
    fun pipelineExist(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线名", required = true)
        @QueryParam("pipelineName")
        pipelineName: String
    ): Result<Boolean>

    @Operation(summary = "拥有权限流水线列表")
    @GET
    // @Path("/projects/{projectId}/hasPermissionList")
    @Path("/{projectId}/hasPermissionList")
    fun hasPermissionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "对应权限", required = true, example = "")
        @QueryParam("permission")
        permission: Permission,
        @Parameter(description = "排除流水线ID", required = false, example = "")
        @QueryParam("excludePipelineId")
        excludePipelineId: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<Pipeline>>

    @Operation(summary = "新建流水线编排")
    @POST
    // @Path("/projects/{projectId}/createPipeline")
    @Path("/{projectId}")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false,
        @Parameter(description = "流水线模型", required = true)
        pipeline: Model
    ): Result<PipelineId>

    @Operation(summary = "用户是否拥有流水线权限")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/hasPermission")
    @Path("/{projectId}/{pipelineId}/hasPermission")
    fun hasPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "权限", required = true)
        @QueryParam("permission")
        permission: Permission
    ): Result<Boolean>

    @Operation(summary = "复制流水线编排")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/copy")
    @Path("/{projectId}/{pipelineId}/copy")
    fun copy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线COPY", required = true)
        pipeline: PipelineCopy
    ): Result<PipelineId>

    @Operation(summary = "编辑流水线编排")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/")
    @Path("/{projectId}/{pipelineId}/")
    fun editPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线模型", required = true)
        pipeline: Model
    ): Result<Boolean>

    @Operation(summary = "编辑流水线编排以及设置")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/saveAll")
    @Path("/{projectId}/{pipelineId}/saveAll")
    fun saveAll(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线模型与设置", required = true)
        @Valid
        modelAndSetting: PipelineModelAndSetting
    ): Result<Boolean>

    @Operation(summary = "保存流水线设置")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/saveSetting")
    @Path("/{projectId}/{pipelineId}/saveSetting")
    fun saveSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>

    @Operation(summary = "启用/禁用流水线（修改流水线的并发设置）")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/lock")
    fun lockPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "开启true/锁定false", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @Operation(summary = "获取流水线编排")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/")
    @Path("/{projectId}/{pipelineId}/")
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("draft")
        @DefaultValue("false")
        includeDraft: Boolean? = false
    ): Result<Model>

    @Operation(summary = "获取流水线编排版本")
    @GET
    @Path("/{projectId}/{pipelineId}/{version}")
    fun getVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线编排版本", required = true)
        @PathParam("version")
        version: Int
    ): Result<Model>

    @Operation(summary = "生成远程执行token")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/remoteToken")
    @Path("/{projectId}/{pipelineId}/remoteToken")
    fun generateRemoteToken(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineRemoteToken>

    @Operation(summary = "删除流水线编排")
    @DELETE
    @Path("/{projectId}/{pipelineId}/")
    fun softDelete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<Boolean>

    @Operation(summary = "批量删除流水线编排")
    @DELETE
    @Path("/batchDelete")
    fun batchDelete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        batchDeletePipeline: BatchDeletePipeline,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<Map<String, Boolean>>

    @Operation(summary = "删除流水线版本")
    @DELETE
    @Path("/{projectId}/{pipelineId}/{version}/")
    fun deleteVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线编排版本", required = true)
        @PathParam("version")
        version: Int
    ): Result<Boolean>

    @Operation(summary = "用户获取视图设置和流水线编排列表")
    @GET
    // @Path("/projects/{projectId}/listViewSettingAndPipelines")
    @Path("/projects/{projectId}/listViewSettingAndPipelines")
    fun listViewSettingAndPipelines(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<PipelineViewAndPipelines>

    @Operation(summary = "用户获取视图流水线编排列表")
    @GET
    @Path("/projects/{projectId}/listViewPipelines")
    fun listViewPipelines(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "流水线排序", required = false, example = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @Parameter(description = "按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?,
        @Parameter(description = "按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String?,
        @Parameter(description = "按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String?,
        @Parameter(description = "按视图过滤", required = false)
        @QueryParam("filterByViewIds")
        filterByViewIds: String? = null,
        @Parameter(description = "用户视图ID,表示用户当前所在视图", required = true)
        @QueryParam("viewId")
        viewId: String,
        @Parameter(description = "排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation?,
        @Parameter(description = "是否展示已删除流水线", required = false)
        @QueryParam("showDelete")
        showDelete: Boolean? = false
    ): Result<PipelineViewPipelinePage<Pipeline>>

    @Operation(summary = "有权限流水线编排列表")
    @GET
    @Path("/{projectId}/")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "流水线排序", required = false, example = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @Parameter(description = "流水线名称", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?
    ): Result<PipelinePage<Pipeline>>

    @Operation(summary = "流水线状态列表")
    @POST
    // @Path("/projects/{projectId}/pipelineStatus")
    @Path("/{projectId}/pipelineStatus")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getPipelineStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Pipeline ID 列表", required = true)
        pipelines: Set<String>
    ): Result<Map<String, PipelineStatus>>

    @Operation(summary = "收藏流水线消息")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/favor")
    @Path("/{projectId}/{pipelineId}/favor")
    fun favor(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否收藏", required = true)
        @QueryParam("type")
        favor: Boolean
    ): Result<Boolean>

    @Operation(summary = "还原流水线编排")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/restore")
    @Path("/{projectId}/{pipelineId}/restore")
    fun restore(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @Operation(summary = "列出等还原回收的流水线列表")
    @GET
    @Path("/{projectId}/pipelineRecycleList")
    fun recycleList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "流水线排序", required = false, example = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @Parameter(description = "排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation?,
        @Parameter(description = "按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?
    ): Result<PipelineViewPipelinePage<PipelineInfo>>

    @Operation(summary = "流水线重命名")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/rename")
    @Path("/{projectId}/{pipelineId}/")
    fun rename(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线名称", required = true)
        name: PipelineName
    ): Result<Boolean>

    @Operation(summary = "获取流水线阶段标签")
    @GET
    @Path("/stageTag")
    fun getStageTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<PipelineStageTag>>

    @Operation(summary = "导出流水线模板")
    @GET
    @Path("{pipelineId}/projects/{projectId}/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Response

    @Operation(summary = "导入流水线模板")
    @POST
    @Path("/projects/{projectId}/upload")
    fun uploadPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        pipelineInfo: PipelineModelAndSetting,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<String?>

    @Operation(summary = "流水线编排版本列表")
    @GET
    @Path("/{projectId}/{pipelineId}/version")
    fun versionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineVersionWithInfo>>

    @Operation(summary = "校验matrix yaml格式")
    @POST
    @Path("/{projectId}/{pipelineId}/matrix/check")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun checkYaml(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "yaml内容", required = true)
        yaml: MatrixPipelineInfo
    ): Result<MatrixPipelineInfo>

    @Operation(summary = "获取列表页列表相关的数目")
    @GET
    @Path("/projects/{projectId}/getCount")
    fun getCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineCount>

    @Operation(summary = "获取继承项目方言的流水线数量")
    @GET
    @Path("{projectId}/countInheritedDialectPipeline")
    fun countInheritedDialectPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Long>

    @Operation(summary = "获取流水线列表根据流水线方言")
    @GET
    @Path("{projectId}/listInheritedDialectPipelines")
    fun listInheritedDialectPipelines(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SQLPage<PipelineIdAndName>>
}
