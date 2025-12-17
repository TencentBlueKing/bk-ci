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

package com.tencent.devops.process.api.template.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceBase
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceCompareResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesRequest
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesTaskDetail
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesTaskResult
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedResp
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_TEMPLATE_INSTANCE_V2", description = "用户-流水模板-实例化-V2")
@Path("/user/template/instances/v2/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineTemplateInstanceV2Resource {

    @Operation(summary = "流水线模板-批量实例化流水线模板-同步")
    @POST
    @Path("/templates/{templateId}")
    fun createTemplateInstances(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板版本", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "创建实例", required = true)
        request: PipelineTemplateInstancesRequest
    ): TemplateOperationRet

    @Operation(summary = "流水线模板-批量实例化流水线模板-异步")
    @POST
    @Path("/templates/async/{templateId}")
    fun asyncCreateTemplateInstances(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板版本", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "创建实例", required = true)
        request: PipelineTemplateInstancesRequest
    ): Result<String>

    @Operation(summary = "异步批量更新流水线模板实例")
    @PUT
    @Path("/templates/{templateId}/async/update")
    fun asyncUpdateTemplateInstances(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板版本", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "更新实例", required = true)
        request: PipelineTemplateInstancesRequest
    ): Result<String>

    @Operation(summary = "列表流水线模板实例")
    @GET
    @Path("/templates/{templateId}")
    fun listTemplateInstances(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "流水线名称", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?,
        @Parameter(description = "更新人", required = false)
        @QueryParam("updater")
        updater: String?,
        @Parameter(description = "状态", required = false)
        @QueryParam("status")
        status: TemplatePipelineStatus?,
        @Parameter(description = "模版版本号", required = false)
        @QueryParam("templateVersion")
        templateVersion: Long?,
        @Parameter(description = "代码库hashId", required = false)
        @QueryParam("repoHashId")
        repoHashId: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int
    ): Result<SQLPage<PipelineTemplateRelatedResp>>

    @Operation(summary = "通过流水线ID获取流水线启动参数")
    @POST
    @Path("/templates/{templateId}/pipelines")
    fun listTemplateInstancesParams(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "创建实例", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String/*pipelineId*/, TemplateInstanceParams>>

    @Operation(summary = "通过ID方式获取模版实例化参数")
    @GET
    @Path("/templates/{templateId}/instanceParamsById")
    fun getTemplateInstanceParamsById(
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
        @QueryParam("version")
        version: Long
    ): Result<TemplateInstanceParams>

    @Operation(summary = "通过Ref方式获取模版实例化参数")
    @GET
    @Path("/templates/{templateId}/instanceParamsByRef")
    fun getTemplateInstanceParamsByRef(
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
    ): Result<TemplateInstanceParams>

    @Operation(summary = "模版实例化发布时版本信息预览")
    @POST
    @Path("/templates/{templateId}/{version}/preFetch")
    fun preFetchTemplateInstance(
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
        @Parameter(description = "更新实例", required = true)
        request: PipelineTemplateInstancesRequest
    ): Result<List<PrefetchReleaseResult>>

    @Operation(summary = "比较模板YAML和流水线实例YAML")
    @GET
    @Path("/{templateId}/compareYaml")
    fun compareTemplateAndPipelineYaml(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板版本", required = true)
        @QueryParam("templateVersion")
        templateVersion: Long,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线版本", required = true)
        @QueryParam("pipelineVersion")
        pipelineVersion: Int,
        @Parameter(description = "是否使用模板设置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean = false
    ): Result<PipelineTemplateInstanceCompareResponse>

    @Operation(summary = "获取模版实例任务结果")
    @GET
    @Path("/task/{baseId}/result")
    fun getTemplateInstanceTaskResult(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("baseId")
        baseId: String
    ): Result<PipelineTemplateInstancesTaskResult>

    @Operation(summary = "获取模版实例任务列表")
    @GET
    @Path("/templates/{templateId}/task")
    fun listTemplateInstanceTask(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(
            description = "任务状态,值为INIT,INSTANCING,SUCCESS,FAILED,多个用,分割,默认值INIT,INSTANCING",
            required = false
        )
        @QueryParam("status")
        status: String?
    ): Result<List<PipelineTemplateInstanceBase>>

    @Operation(summary = "重试失败的模版实例任务,返回新的任务ID")
    @POST
    @Path("/task/{baseId}/retry")
    fun retryTemplateInstanceTask(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("baseId")
        baseId: String
    ): Result<String>

    @Operation(summary = "获取失败的实例化任务详情")
    @GET
    @Path("/task/{baseId}/detail")
    fun getTemplateInstanceTaskDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("baseId")
        baseId: String,
        @Parameter(
            description = "任务状态,值为INIT,INSTANCING,SUCCESS,FAILED,多个用,分割,默认值INIT,INSTANCING",
            required = false
        )
        @QueryParam("status")
        status: String?
    ): Result<PipelineTemplateInstancesTaskDetail>
}
