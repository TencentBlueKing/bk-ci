package com.tencent.devops.process.api.template.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.pojo.template.HighlightType
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.PipelineTemplateListResponse
import com.tencent.devops.process.pojo.template.PipelineTemplateListSimpleResponse
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineRefInfo
import com.tencent.devops.process.pojo.template.v2.PTemplateSource2Count
import com.tencent.devops.process.pojo.template.v2.PTemplateTransferBody
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCompareResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCopyCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCustomCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDetailsResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftReleaseReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftSaveReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateStrategyUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PreFetchTemplateReleaseResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
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

@Tag(name = "USER_PIPELINE_TEMPLATE_V2", description = "用户-流水线-模板-V2")
@Path("/user/pipeline/template/v2/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineTemplateV2Resource {
    @Operation(summary = "新建流水线模板")
    @POST
    @Path("/create")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateCustomCreateReq
    ): Result<DeployTemplateResult>

    @Operation(summary = "研发商店导入模板")
    @POST
    @Path("/create/market")
    fun createByMarket(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateMarketCreateReq
    ): Result<DeployTemplateResult>

    @Operation(summary = "复制")
    @POST
    @Path("/copy/")
    fun copy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateCopyCreateReq
    ): Result<DeployTemplateResult>

    @Operation(summary = "删除流水线模板")
    @DELETE
    @Path("/{templateId}/delete/")
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>

    @Operation(summary = "保存流水线模板草稿")
    @PUT
    @Path("/saveDraft")
    fun saveDraft(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateDraftSaveReq
    ): Result<DeployTemplateResult>

    @Operation(summary = "获取模板列表")
    @POST
    @Path("/list/")
    fun listTemplateInfos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateCommonCondition
    ): Result<SQLPage<PipelineTemplateListResponse>>

    @Operation(summary = "获取模板列表-简化")
    @POST
    @Path("/list/simple/")
    fun listTemplateSimpleInfos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateCommonCondition
    ): Result<SQLPage<PipelineTemplateListSimpleResponse>>

    @Operation(summary = "创建流水线-获取模版列表")
    @GET
    @Path("/allTemplates")
    fun listAllTemplates(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<OptionalTemplateList>

    @Operation(summary = "查看模板详情")
    @GET
    @Path("/{templateId}/{version}/details/")
    fun getTemplateDetails(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "版本", required = false)
        @PathParam("version")
        version: Long
    ): Result<PipelineTemplateDetailsResponse>

    @Operation(summary = "查看模板最新详情")
    @GET
    @Path("/{templateId}/latest/details/")
    fun getLatestTemplateDetails(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<PipelineTemplateDetailsResponse>

    @Operation(summary = "根据引用查看模版详情")
    @GET
    @Path("/{templateId}/ref/details/")
    fun getRefTemplateDetails(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模版引用,可以是分支/tag/commit", required = true)
        @QueryParam("ref")
        ref: String
    ): Result<PipelineTemplateDetailsResponse>

    @Operation(summary = "根据流水线版本查看关联的模版详情,用户流水线参数、触发器和设置改自定义时,查看模版的值")
    @GET
    @Path("/pipelines/{pipelineId}/versions/{version}/related/details")
    fun getPipelineRelatedTemplateDetails(
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
    ): Result<PipelineTemplateDetailsResponse?>

    @Operation(summary = "根据流水线版本查看关联的模版信息")
    @GET
    @Path("/pipelines/{pipelineId}/versions/{version}/related/info")
    fun getPipelineRelatedTemplateInfo(
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
    ): Result<PTemplatePipelineRefInfo?>

    @Operation(summary = "查看模板基本信息")
    @GET
    @Path("/{templateId}/info/")
    fun getTemplateInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<PipelineTemplateInfoResponse>

    @Operation(summary = "获取项目模板类型对应的数量")
    @GET
    @Path("/getType2Count/")
    fun getType2Count(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Map<String, Int>>

    @Operation(summary = "获取项目模板来源对应的数量")
    @POST
    @Path("/getSource2Count/")
    fun getSource2Count(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询请求体", required = true)
        commonCondition: PipelineTemplateCommonCondition
    ): Result<PTemplateSource2Count>

    @Operation(summary = "查看模板的版本历史")
    @POST
    @Path("/{templateId}/versions/")
    fun getTemplateVersions(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "请求体", required = false)
        request: PipelineTemplateResourceCommonCondition
    ): Result<Page<PipelineVersionSimple>>

    @Operation(summary = "版本对比")
    @GET
    @Path("/{templateId}/compare/")
    fun compare(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "基准版本", required = false)
        @QueryParam("baseVersion")
        baseVersion: Long,
        @Parameter(description = "比较版本", required = false)
        @QueryParam("comparedVersion")
        comparedVersion: Long
    ): Result<PipelineTemplateCompareResponse>

    @Operation(summary = "草稿发布为正式版本的信息预览")
    @GET
    @Path("/{templateId}/releaseVersion/{version}/prefetch")
    fun preFetchDraftVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模版版本", required = true)
        @PathParam("version")
        version: Long,
        @Parameter(description = "自定义版本名称", required = false)
        @QueryParam("customVersionName")
        customVersionName: String? = null,
        @Parameter(description = "是否开启PAC", required = false)
        @QueryParam("enablePac")
        enablePac: Boolean = false,
        @Parameter(description = "提交动作", required = false)
        @QueryParam("targetAction")
        targetAction: CodeTargetAction? = null,
        @Parameter(description = "代码库hashId", required = false)
        @QueryParam("repoHashId")
        repoHashId: String? = null,
        @Parameter(description = "指定提交的分支", required = false)
        @QueryParam("targetBranch")
        targetBranch: String? = null
    ): Result<PreFetchTemplateReleaseResult>

    @Operation(summary = "将当前草稿发布为正式版本")
    @POST
    @Path("{templateId}/releaseVersion/{version}")
    fun releaseDraftVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模版版本", required = true)
        @PathParam("version")
        version: Long,
        @Parameter(description = "流水线模版发布请求体", required = true)
        request: PipelineTemplateDraftReleaseReq
    ): Result<DeployTemplateResult>

    @Operation(summary = "获取模板操作日志列表（分页）")
    @GET
    @Path("{templateId}/operationLog")
    fun getPipelineOperationLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
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

    @Operation(summary = "获取模板操作人列表（分页）")
    @GET
    @Path("/{templateId}/operatorList")
    fun operatorList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<List<String>>

    @Operation(summary = "回滚到指定的历史版本并覆盖草稿")
    @POST
    @Path("{templateId}/rollbackDraft")
    fun rollbackDraftFromVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "回回滚目标版本", required = true)
        @QueryParam("version")
        version: Long
    ): Result<DeployTemplateResult>

    @Operation(summary = "删除模版版本")
    @DELETE
    @Path("{templateId}/{version}/")
    fun deleteVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模版版本", required = true)
        @PathParam("version")
        version: Long
    ): Result<Boolean>

    @Operation(summary = "是否有模板特定权限")
    @GET
    @Path("/hasPipelineTemplatePermission")
    fun hasPipelineTemplatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @Parameter(description = "操作", required = true)
        @QueryParam("permission")
        permission: AuthPermission
    ): Result<Boolean>

    @Operation(summary = "是否开启模板管理权限")
    @GET
    @Path("/enableTemplatePermissionManage")
    fun enableTemplatePermissionManage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "转化")
    @POST
    @Path("/transfer")
    fun transfer(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "转化格式，若yaml转model，参数填写YAML；model转yaml，填写Model", required = true)
        @QueryParam("storageType")
        storageType: PipelineStorageType,
        @Parameter(description = "请求体", required = true)
        body: PTemplateTransferBody
    ): Result<PTemplateModelTransferResult?>

    @Operation(summary = "导出流水线模板")
    @GET
    @Path("{templateId}/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "导出的目标版本", required = false)
        @QueryParam("version")
        version: Long?
    ): Response

    @Operation(summary = "转化模板为自定义")
    @POST
    @Path("{templateId}/transformTemplateToCustom")
    fun transformTemplateToCustom(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>

    @Operation(summary = "更新升级策略")
    @PUT
    @Path("{templateId}/updateUpgradeStrategy")
    fun updateUpgradeStrategy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "升级策略", required = false)
        request: PipelineTemplateStrategyUpdateInfo
    ): Result<Boolean>

    @Operation(summary = "预览模板")
    @GET
    @Path("/{templateId}/preview")
    fun previewTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板版本", required = false)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "高亮类型", required = false)
        @QueryParam("highlightType")
        highlightType: HighlightType?
    ): Result<TemplatePreviewDetail>
}
