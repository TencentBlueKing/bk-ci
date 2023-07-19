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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelUpdate
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.Permission
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE"], description = "服务-流水线资源")
@Path("/service/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServicePipelineResource {

    @ApiOperation("新建流水线编排")
    @POST
    // @Path("/projects/{projectId}/createPipeline")
    @Path("/{projectId}/")
    fun create(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线模型", required = true)
        pipeline: Model,
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false
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
        pipeline: Model,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否修改最后修改人", required = false)
        @QueryParam("updateLastModifyUser")
        @DefaultValue("true")
        updateLastModifyUser: Boolean? = true
    ): Result<Boolean>

    @ApiOperation("复制流水线编排")
    @POST
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

    @ApiOperation("导入新流水线, 包含流水线编排和设置")
    @POST
    @Path("/projects/{projectId}/pipeline_upload")
    fun uploadPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线模型与设置", required = true)
        @Valid
        modelAndSetting: PipelineModelAndSetting,
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false
    ): Result<PipelineId>

    @ApiOperation("更新流水线编排和设置")
    @PUT
    @Path("/projects/{projectId}/{pipelineId}/pipeline_edit")
    fun updatePipeline(
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
        modelAndSetting: PipelineModelAndSetting,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<DeployPipelineResult>

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
        pipelineId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Model>

    @ApiOperation("获取流水线编排(带权限校验)")
    @GET
    @Path("/{projectId}/{pipelineId}/withPermission")
    fun getWithPermission(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否进行权限校验", required = true)
        @QueryParam("checkPermission")
        checkPermission: Boolean
    ): Result<Model>

    @ApiOperation("获取流水线编排(带权限校验)")
    @GET
    @Path("/{projectId}/{pipelineId}/get_setting_with_permission")
    fun getSettingWithPermission(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否进行权限校验", required = true)
        @QueryParam("checkPermission")
        checkPermission: Boolean
    ): Result<PipelineSetting>

    @ApiOperation("批量获取流水线编排与配置")
    @POST
    @Path("/{projectId}/batchGet")
    fun getBatch(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID列表", required = true)
        pipelineIds: List<String>,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<List<Pipeline>>

    @ApiOperation("保存流水线设置")
    @PUT
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
        @ApiParam("是否修改最后修改人", required = false)
        @QueryParam("updateLastModifyUser")
        @DefaultValue("true")
        updateLastModifyUser: Boolean? = true,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        @DefaultValue("BS")
        channelCode: ChannelCode? = ChannelCode.BS,
        @ApiParam(value = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>

    @ApiOperation("获取流水线基本信息")
    @GET
    @Path("/{projectId}/{pipelineId}/getPipelineInfo")
    fun getPipelineInfo(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<PipelineInfo?>

    @ApiOperation("删除流水线编排")
    @DELETE
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/")
    @Path("/{projectId}/{pipelineId}/")
    fun delete(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @ApiOperation("流水线编排列表")
    @GET
    // @Path("/projects/{projectId}/listPipelines")
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
        page: Int? = null,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS,
        @ApiParam("是否校验权限", required = false)
        @QueryParam("checkPermission")
        checkPermission: Boolean? = true
    ): Result<Page<Pipeline>>

    @ApiOperation("获取流水线状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/status")
    @Path("/{projectId}/{pipelineId}/status")
    fun status(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("channel", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS
    ): Result<Pipeline?>

    @ApiOperation("获取流水线完整状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/allStatus")
    @Path("/{projectId}/{pipelineId}/allStatus")
    fun getAllstatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<Pipeline>?>

    @ApiOperation("流水线是否运行中")
    @GET
    // @Path("/projects/{projectId}/builds/{buildId}/running")
    @Path("/{projectId}/build/{buildId}/running")
    fun isPipelineRunning(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @ApiOperation("流水线是否运行中（包括审核、等待等状态）")
    @GET
    @Path("/{projectId}/build/{buildId}/isrunning")
    fun isRunning(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @ApiOperation("流水线个数统计")
    @GET
    @Path("/count")
    fun count(
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: Set<String>?,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Long>

    @ApiOperation("根据流水线id获取流水线名字")
    @POST
    // @Path("/projects/{projectId}/getPipelines")
    @Path("/{projectId}/getPipelines")
    fun getPipelineByIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<List<SimplePipeline>>

    @ApiOperation("根据流水线id获取流水线名字")
    @POST
    // @Path("/projects/{projectId}/getPipelineNames")
    @Path("/{projectId}/getPipelineNames")
    fun getPipelineNameByIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("根据构建id，获取build num")
    @POST
    // @Path("/getBuildNoByIds")
    @Path("/buildIds/getBuildNo")
    fun getBuildNoByBuildIds(
        @ApiParam("构建id", required = true)
        buildIds: Set<String>,
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: String? = null
    ): Result<Map<String/*buildId*/, String/*buildNo*/>>

    @ApiOperation("流水线重命名")
    @POST
    @Path("/{pipelineId}/projects/{projectId}/rename")
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

    @ApiOperation("还原流水线编排")
    @PUT
    @Path("/{pipelineId}/projects/{projectId}/restore")
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

    @ApiOperation("获取项目下流水线Id列表")
    @PUT
    @Path("/projects/{projectCode}/idList")
    fun getProjectPipelineIds(
        @ApiParam("项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<PipelineIdInfo>>

    @ApiOperation("获取项目下流水线Id")
    @PUT
    @Path("/projects/{projectCode}/pipelines/{pipelineId}/id")
    fun getPipelineId(
        @ApiParam("项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineIdInfo?>

    @ApiOperation("根据流水线id获取流水线信息")
    @GET
    @Path("/pipelines/{pipelineId}")
    fun getPipelineInfoByPipelineId(
        @ApiParam("流水线id列表", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<SimplePipeline?>?

    @ApiOperation("根据项目ID获取流水线标签关系列表")
    @POST
    @Path("/labelinfos/list")
    fun getPipelineLabelInfos(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        projectIds: List<String>
    ): Result<List<PipelineLabelRelateInfo>>

    @ApiOperation("根据流水线名称搜索")
    @GET
    @Path("/projects/{projectId}/search_by_name")
    fun searchByName(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("搜索名称")
        @QueryParam("pipelineName")
        pipelineName: String?
    ): Result<List<PipelineIdAndName>>

    @ApiOperation("批量更新modelName")
    @POST
    @Path("/batch/pipeline/modelName")
    fun batchUpdateModelName(
        modelUpdateList: List<ModelUpdate>
    ): Result<List<ModelUpdate>>

    @ApiOperation("根据自增id获取流水线信息")
    @GET
    @Path("/projects{projectId}/pipelines/{id}/info")
    fun getPipelineInfobyAutoId(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("id")
        @ApiParam(value = "流水线自增id", required = true)
        id: Long
    ): Result<SimplePipeline?>

    @ApiOperation("拥有权限流水线列表")
    @GET
    @Path("/hasPermissionList")
    fun hasPermissionList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
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
}
