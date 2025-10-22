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

package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "SERVICE_TEMPLATE_INSTANCE", description = "服务-流水模板-实例化资源")
@Path("/service/templateInstances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceTemplateInstanceResource {

    @Operation(summary = "创建流水线模板")
    @POST
    @Path("/projects/{projectId}/templates/{templateId}")
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
        @Parameter(description = "是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @Parameter(description = "创建实例", required = true)
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet

    @Operation(summary = "批量更新流水线模板实例")
    @PUT
    @Path("/projects/{projectId}/templates/{templateId}")
    fun updateTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "版本名", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @Parameter(description = "模板实例", required = true)
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet

    @Operation(summary = "批量更新流水线模板实例")
    @PUT
    @Path("/projects/{projectId}/templates/{templateId}/updateTemplate")
    fun updateTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "版本名", required = true)
        @QueryParam("versionName")
        versionName: String,
        @Parameter(description = "是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @Parameter(description = "模板实例", required = true)
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet

    @Operation(summary = "通过流水线ID获取流水线启动参数")
    @POST
    @Path("/projects/{projectId}/templates/{templateId}/pipelines")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
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
        @Parameter(description = "模板版本", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "创建实例", required = true)
        pipelineIds: List<PipelineId>
    ): Result<Map<String/*pipelineId*/, TemplateInstanceParams>>

    @Operation(summary = "列表流水线模板实例")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}")
    fun listTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页多少条", required = false, example = "30")
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "名字搜索的关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @Parameter(description = "排序字段", required = false)
        @QueryParam("sortType")
        sortType: TemplateSortTypeEnum?,
        @Parameter(description = "是否降序", required = false)
        @QueryParam("desc")
        desc: Boolean?
    ): Result<TemplateInstancePage>

    @Operation(summary = "查询流水线模板实例总数")
    @POST
    @Path("/projects/{projectId}/countTemplateInstance")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun countTemplateInstance(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        templateIds: Collection<String>
    ): Result<Int>

    @Operation(summary = "查询流水线模板实例总数")
    @POST
    @Path("/projects/{projectId}/countTemplateInstanceDetail")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun countTemplateInstanceDetail(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        templateIds: Collection<String>
    ): Result<Map<String, Int>>
}
