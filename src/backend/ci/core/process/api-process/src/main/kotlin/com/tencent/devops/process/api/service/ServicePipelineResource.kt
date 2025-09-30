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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelUpdate
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.Permission
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineRemoteToken
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
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

@Tag(name = "SERVICE_PIPELINE", description = "服务-流水线资源")
@Path("/service/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServicePipelineResource {

    @Operation(summary = "新建流水线编排")
    @POST
    // @Path("/projects/{projectId}/createPipeline")
    @Path("/{projectId}/")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型", required = true)
        pipeline: Model,
        @Parameter(description = "渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false
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
        pipeline: Model,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "是否修改最后修改人", required = false)
        @QueryParam("updateLastModifyUser")
        @DefaultValue("true")
        updateLastModifyUser: Boolean? = true
    ): Result<Boolean>

    @Operation(summary = "复制流水线编排")
    @POST
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

    @Operation(summary = "导入新流水线, 包含流水线编排和设置")
    @POST
    @Path("/projects/{projectId}/pipeline_upload")
    fun uploadPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型与设置", required = true)
        @Valid
        modelAndSetting: PipelineModelAndSetting,
        @Parameter(description = "渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false
    ): Result<PipelineId>

    @Operation(summary = "更新流水线编排和设置")
    @PUT
    @Path("/projects/{projectId}/{pipelineId}/pipeline_edit")
    fun updatePipeline(
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
        modelAndSetting: PipelineModelAndSetting,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<DeployPipelineResult>

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
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "版本", required = false)
        @QueryParam("version")
        version: Int? = null,
        @Parameter(description = "是否校验权限", required = false)
        @QueryParam("checkPermission")
        @DefaultValue("false")
        checkPermission: Boolean = false,
        @Parameter(description = "是否查询草稿", required = false)
        @QueryParam("includeDraft")
        @DefaultValue("false")
        includeDraft: Boolean = false
    ): Result<Model>

    @Operation(summary = "获取流水线编排(带权限校验)")
    @GET
    @Path("/{projectId}/{pipelineId}/withPermission")
    fun getWithPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "是否进行权限校验", required = true)
        @QueryParam("checkPermission")
        checkPermission: Boolean
    ): Result<Model>

    @Operation(summary = "获取流水线编排(带权限校验)")
    @GET
    @Path("/{projectId}/{pipelineId}/get_setting_with_permission")
    fun getSettingWithPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "是否进行权限校验", required = true)
        @QueryParam("checkPermission")
        checkPermission: Boolean
    ): Result<PipelineSetting>

    @Operation(summary = "批量获取流水线编排与配置")
    @POST
    @Path("/{projectId}/batchGet")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getBatch(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID列表", required = true)
        pipelineIds: List<String>,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<List<Pipeline>>

    @Operation(summary = "保存流水线设置")
    @PUT
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
        @Parameter(description = "是否修改最后修改人", required = false)
        @QueryParam("updateLastModifyUser")
        @DefaultValue("true")
        updateLastModifyUser: Boolean? = true,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        @DefaultValue("BS")
        channelCode: ChannelCode? = ChannelCode.BS,
        @Parameter(description = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>

    @Operation(summary = "获取流水线基本信息")
    @GET
    @Path("/{projectId}/{pipelineId}/getPipelineInfo")
    fun getPipelineInfo(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "渠道号，不指定则为空", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<PipelineInfo?>

    @Operation(summary = "删除流水线编排")
    @DELETE
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/")
    @Path("/{projectId}/{pipelineId}/")
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @Parameter(description = "是否检查权限", required = false)
        @QueryParam("checkFlag")
        @DefaultValue("true")
        checkFlag: Boolean? = true
    ): Result<Boolean>

    @Operation(summary = "流水线编排列表")
    @GET
    // @Path("/projects/{projectId}/listPipelines")
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
        page: Int? = null,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS,
        @Parameter(description = "是否校验权限", required = false)
        @QueryParam("checkPermission")
        checkPermission: Boolean? = true
    ): Result<Page<Pipeline>>

    @Operation(summary = "获取流水线状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/status")
    @Path("/{projectId}/{pipelineId}/status")
    fun status(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "channel", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS
    ): Result<Pipeline?>

    @Operation(summary = "获取流水线完整状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/allStatus")
    @Path("/{projectId}/{pipelineId}/allStatus")
    fun getAllstatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<Pipeline>?>

    @Operation(summary = "流水线是否运行中")
    @GET
    // @Path("/projects/{projectId}/builds/{buildId}/running")
    @Path("/{projectId}/build/{buildId}/running")
    fun isPipelineRunning(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @Operation(summary = "流水线是否运行中（包括审核、等待等状态）")
    @GET
    @Path("/{projectId}/build/{buildId}/isrunning")
    fun isRunning(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @Operation(summary = "流水线个数统计")
    @GET
    @Path("/count")
    fun count(
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: Set<String>?,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Long>

    @Operation(summary = "根据流水线id获取流水线名字")
    @POST
    // @Path("/projects/{projectId}/getPipelines")
    @Path("/{projectId}/getPipelines")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getPipelineByIds(
        @Parameter(description = "项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<List<SimplePipeline>>

    @Operation(summary = "根据流水线id获取流水线名字")
    @POST
    // @Path("/projects/{projectId}/getPipelineNames")
    @Path("/{projectId}/getPipelineNames")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getPipelineNameByIds(
        @Parameter(description = "项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @Operation(summary = "根据构建id，获取build num")
    @POST
    // @Path("/getBuildNoByIds")
    @Path("/buildIds/getBuildNo")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getBuildNoByBuildIds(
        @Parameter(description = "构建id", required = true)
        buildIds: Set<String>,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String? = null
    ): Result<Map<String/*buildId*/, String/*buildNo*/>>

    @Operation(summary = "流水线重命名")
    @POST
    @Path("/{pipelineId}/projects/{projectId}/rename")
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

    @Operation(summary = "还原流水线编排")
    @PUT
    @Path("/{pipelineId}/projects/{projectId}/restore")
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

    @Operation(summary = "获取项目下流水线Id列表")
    @PUT
    @Path("/projects/{projectCode}/idList")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getProjectPipelineIds(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<PipelineIdInfo>>

    @Operation(summary = "获取项目下流水线Id")
    @PUT
    @Path("/projects/{projectCode}/pipelines/{pipelineId}/id")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getPipelineId(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineIdInfo?>

    @Operation(summary = "根据流水线id获取流水线信息")
    @GET
    @Path("/pipelines/{pipelineId}")
    fun getPipelineInfoByPipelineId(
        @Parameter(description = "流水线id列表", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<SimplePipeline?>?

    @Operation(summary = "根据项目ID获取流水线标签关系列表")
    @POST
    @Path("/labelinfos/list")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun getPipelineLabelInfos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        projectIds: List<String>
    ): Result<List<PipelineLabelRelateInfo>>

    @Operation(summary = "根据流水线名称搜索")
    @GET
    @Path("/projects/{projectId}/search_by_name")
    fun searchByName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "搜索名称")
        @QueryParam("pipelineName")
        pipelineName: String?
    ): Result<List<PipelineIdAndName>>

    @Operation(summary = "根据流水线名称搜索")
    @GET
    @Path("/projects/{projectId}/paging_search_by_name")
    fun pagingSearchByName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "搜索名称")
        @QueryParam("pipelineName")
        pipelineName: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<PipelineViewPipelinePage<PipelineInfo>>

    @Operation(summary = "批量更新modelName")
    @POST
    @Path("/batch/pipeline/modelName")
    fun batchUpdateModelName(
        modelUpdateList: List<ModelUpdate>
    ): Result<List<ModelUpdate>>

    @Operation(summary = "根据自增id获取流水线信息")
    @GET
    @Path("/projects{projectId}/pipelines/{id}/info")
    fun getPipelineInfobyAutoId(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("id")
        @Parameter(description = "流水线自增id", required = true)
        id: Long
    ): Result<SimplePipeline?>

    @Operation(summary = "拥有权限流水线列表")
    @GET
    @Path("/hasPermissionList")
    fun hasPermissionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
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

    @Operation(summary = "生成远程执行token")
    @PUT
    @Path("/{projectId}/{pipelineId}/remote_token")
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
}
