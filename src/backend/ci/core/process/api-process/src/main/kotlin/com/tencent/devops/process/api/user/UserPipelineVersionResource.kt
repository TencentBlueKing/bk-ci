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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.pojo.BuildNoUpdateReq
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "USER_PIPELINE_VERSION", description = "用户-流水线版本管理")
@Path("/user/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList", "TooManyFunctions")
interface UserPipelineVersionResource {

    @Operation(summary = "获取流水线信息（含草稿）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/detail")
    fun getPipelineVersionDetail(
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
    ): Result<PipelineDetail>

    @Operation(summary = "草稿发布为正式版本的信息预览")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/releaseVersion/{version}/prefetch")
    fun preFetchDraftVersion(
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
        version: Int,
        @Parameter(description = "提交动作", required = false)
        @QueryParam("targetAction")
        targetAction: CodeTargetAction? = null,
        @Parameter(description = "代码库hashId", required = false)
        @QueryParam("repoHashId")
        repoHashId: String? = null,
        @Parameter(description = "指定提交的分支", required = false)
        @QueryParam("targetBranch")
        targetBranch: String? = null
    ): Result<PrefetchReleaseResult>

    @Operation(summary = "将当前草稿发布为正式版本")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/releaseVersion/{version}")
    fun releaseDraftVersion(
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
        version: Int,
        @Parameter(description = "发布流水线版本请求", required = true)
        request: PipelineVersionReleaseRequest
    ): Result<DeployPipelineResult>

    @Operation(summary = "通过指定模板创建流水线")
    @POST
    @Path("/projects/{projectId}/createPipelineWithTemplate")
    fun createPipelineFromTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型实例请求", required = true)
        request: TemplateInstanceCreateRequest
    ): Result<DeployPipelineResult>

    @Operation(summary = "获取流水线指定版本的两种编排")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/versions/{version}")
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
        version: Int,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<PipelineVersionWithModel>

    @Operation(summary = "触发前配置")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/previewCode")
    fun previewCode(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线版本号", required = false)
        @QueryParam("version")
        version: Int?
    ): Result<PreviewResponse>

    @Operation(summary = "保存流水线编排草稿")
    @POST
    @Path("/projects/{projectId}/saveDraft")
    fun savePipelineDraft(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型与设置", required = true)
        @Valid
        modelAndYaml: PipelineVersionWithModelRequest
    ): Result<DeployPipelineResult>

    @Operation(summary = "获取流水线编排创建人列表（分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/creatorList")
    fun versionCreatorList(
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
    ): Result<Page<String>>

    @Operation(summary = "流水线编排版本列表（搜索、分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/versions")
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
        @Parameter(description = "跳转定位的版本号", required = false)
        @QueryParam("fromVersion")
        fromVersion: Int? = null,
        @Parameter(description = "搜索字段：版本名包含字符", required = false)
        @QueryParam("versionName")
        versionName: String? = null,
        @Parameter(description = "是否包括草稿的全量版本", required = false)
        @QueryParam("includeDraft")
        includeDraft: Boolean? = true,
        @Parameter(description = "搜索字段：创建人", required = false)
        @QueryParam("creator")
        creator: String? = null,
        @Parameter(description = "搜索字段：变更说明", required = false)
        @QueryParam("description")
        description: String? = null,
        @Parameter(description = "仅查询可执行构建的版本: 最新正式版本或者分支版本", required = false)
        @QueryParam("buildOnly")
        buildOnly: Boolean? = false,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "5")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<Page<PipelineVersionSimple>>

    @Operation(summary = "获取指定版本号的流水线编排版本信息")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/versions/{version}/info")
    fun getVersionInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "跳转定位的版本号", required = false)
        @PathParam("version")
        version: Int,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<PipelineVersionSimple>

    @Operation(summary = "获取流水线操作日志列表（分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/operationLog")
    fun getPipelineOperationLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "搜索字段：创建人", required = false)
        @QueryParam("creator")
        creator: String? = null,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<Page<PipelineOperationDetail>>

    @Operation(summary = "获取流水线操作人列表（分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/operatorList")
    fun operatorList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<String>>

    @Operation(summary = "回滚到指定的历史版本并覆盖草稿")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/rollbackDraft")
    fun rollbackDraftFromVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "回回滚目标版本", required = true)
        @QueryParam("version")
        version: Int
    ): Result<PipelineVersionSimple>

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
        @Parameter(description = "导出的目标版本", required = false)
        @QueryParam("version")
        version: Int?,
        @Parameter(description = "导出的数据类型", required = false)
        @QueryParam("storageType")
        storageType: String?
    ): Response

    @Operation(summary = "重置流水线推荐版本号")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/updateBuildNo")
    fun updateBuildNo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线构建推荐版本号更新", required = true)
        buildNo: BuildNoUpdateReq
    ): Result<Boolean>
}
