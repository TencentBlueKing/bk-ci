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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.pojo.BuildNoUpdateReq
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Tag(name = "OPENAPI_PIPELINE_VERSION_V4", description = "OPENAPI-流水线版本管理资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwPipelineVersionResourceV4 {

    @Operation(summary = "获取流水线信息（含草稿）", tags = ["v4_app_pipeline_detail", "v4_user_pipeline_detail"])
    @GET
    @Path("/pipelines/{pipelineId}/pipeline_detail")
    fun getPipelineVersionDetail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineDetail>

    @Operation(
        summary = "将当前草稿发布为正式版本",
        tags = ["v4_app_pipeline_release_prefetch", "v4_user_pipeline_release_prefetch"]
    )
    @GET
    @Path("/pipelines/{pipelineId}/release_prefetch")
    fun preFetchDraftVersion(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线编排版本", required = true)
        @QueryParam("version")
        version: Int
    ): Result<PrefetchReleaseResult>

    @Operation(summary = "将当前模板发布为正式版本", tags = ["v4_app_pipeline_release", "v4_user_pipeline_release"])
    @POST
    @Path("/pipelines/{pipelineId}/release_version")
    fun releaseDraftVersion(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线编排版本", required = true)
        @QueryParam("version")
        version: Int,
        @Parameter(description = "发布流水线版本请求", required = true)
        request: PipelineVersionReleaseRequest
    ): Result<DeployPipelineResult>

    @Operation(
        summary = "通过指定模板创建流水线",
        tags = ["v4_app_pipeline_create_with_template", "v4_user_pipeline_create_with_template"]
    )
    @POST
    @Path("/create_with_template")
    fun createPipelineFromTemplate(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型实例请求", required = true)
        request: TemplateInstanceCreateRequest
    ): Result<DeployPipelineResult>

    @Operation(
        summary = "获取流水线指定版本的两种编排",
        tags = ["v4_app_pipeline_get_version", "v4_user_pipeline_get_version"]
    )
    @GET
    @Path("/pipelines/{pipelineId}/get_version")
    fun getVersion(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线编排版本", required = true)
        @QueryParam("version")
        version: Int
    ): Result<PipelineVersionWithModel>

    @Operation(summary = "触发前配置", tags = ["v4_app_pipeline_preview_code", "v4_user_pipeline_preview_code"])
    @GET
    @Path("/pipelines/{pipelineId}/preview_code")
    fun previewCode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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

    @Operation(
        summary = "保存或创建流水线编排草稿",
        tags = ["v4_app_pipeline_save_draft", "v4_user_pipeline_save_draft"]
    )
    @POST
    @Path("/save_draft")
    fun savePipelineDraft(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型与设置", required = true)
        @Valid
        modelAndYaml: PipelineVersionWithModelRequest
    ): Result<DeployPipelineResult>

    @Operation(
        summary = "获取流水线编排创建人列表（分页）",
        tags = ["v4_app_pipeline_creator_list", "v4_user_pipeline_creator_list"]
    )
    @GET
    @Path("/pipelines/{pipelineId}/creator_list")
    fun versionCreatorList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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

    @Operation(
        summary = "流水线编排版本列表（搜索、分页）",
        tags = ["v4_app_pipeline_version_list", "v4_user_pipeline_version_list"]
    )
    @GET
    @Path("/pipelines/{pipelineId}/version_list")
    fun versionList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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
        @Parameter(description = "搜索字段：创建人", required = false)
        @QueryParam("creator")
        creator: String? = null,
        @Parameter(description = "搜索字段：变更说明", required = false)
        @QueryParam("description")
        description: String? = null,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "5")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineVersionWithInfo>>

    @Operation(
        summary = "获取流水线操作日志列表（分页）",
        tags = ["v4_app_pipeline_operation_log", "v4_user_pipeline_operation_log"]
    )
    @GET
    @Path("/pipelines/{pipelineId}/operation_log")
    fun getPipelineOperationLogs(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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
        pageSize: Int?
    ): Result<Page<PipelineOperationDetail>>

    @Operation(
        summary = "获取流水线操作人列表（分页）",
        tags = ["v4_app_pipeline_operator_list", "v4_user_pipeline_operator_list"]
    )
    @GET
    @Path("/pipelines/{pipelineId}/operator_list")
    fun operatorList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<String>>

    @Operation(
        summary = "回滚到指定的历史版本并覆盖草稿",
        tags = ["v4_app_pipeline_rollback_draft", "v4_user_pipeline_rollback_draft"]
    )
    @POST
    @Path("/pipelines/{pipelineId}/rollback_draft")
    fun rollbackDraftFromVersion(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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

    @Operation(summary = "导出流水线模板", tags = ["v4_app_pipeline_export", "v4_user_pipeline_export"])
    @GET
    @Path("/pipelines/{pipelineId}/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportPipeline(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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

    @Operation(
        summary = "重置流水线推荐版本号",
        tags = ["v4_app_pipeline_update_build_no", "v4_user_pipeline_update_build_no"]
    )
    @POST
    @Path("/pipelines/{pipelineId}/update_build_no")
    fun updateBuildNo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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
