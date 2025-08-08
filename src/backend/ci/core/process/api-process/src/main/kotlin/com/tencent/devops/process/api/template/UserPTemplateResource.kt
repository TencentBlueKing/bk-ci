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
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.pojo.PTemplateOrderByType
import com.tencent.devops.process.pojo.PTemplateSortType
import com.tencent.devops.process.pojo.template.CopyTemplateReq
import com.tencent.devops.process.pojo.template.HighlightType
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.SaveAsTemplateReq
import com.tencent.devops.process.pojo.template.TemplateId
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.TemplateType
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

@Tag(name = "USER_TEMPLATE", description = "用户-流水模板资源")
@Path("/user/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPTemplateResource {

    @Operation(summary = "创建流水线模板")
    @POST
    @Path("/projects/{projectId}/templates")
    fun createTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板", required = true)
        template: Model
    ): Result<TemplateId>

    @Operation(summary = "删除流水线模板")
    @DELETE
    @Path("/projects/{projectId}/templates/{templateId}")
    fun deleteTemplate(
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

    @Operation(summary = "删除流水线模板")
    @DELETE
    @Path("/projects/{projectId}/templates/{templateId}/versions/{version}")
    fun deleteTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: Long
    ): Result<Boolean>

    @Operation(summary = "删除流水线模板")
    @DELETE
    @Path("/projects/{projectId}/templates/{templateId}/deletetemplate")
    fun deleteTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 34)
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        @BkField(minLength = 1, maxLength = 32)
        templateId: String,
        @Parameter(description = "版本号", required = true)
        @QueryParam("versionName")
        @BkField(minLength = 1, maxLength = 64)
        versionName: String
    ): Result<Boolean>

    @Operation(summary = "更新流水线模板")
    @PUT
    @Path("/projects/{projectId}/templates/{templateId}")
    fun updateTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 34)
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        @BkField(minLength = 1, maxLength = 32)
        templateId: String,
        @Parameter(description = "版本名", required = true)
        @QueryParam("versionName")
        @BkField(minLength = 1, maxLength = 64)
        versionName: String,
        @Parameter(description = "模板", required = true)
        template: Model
    ): Result<Boolean>

    @Operation(summary = "模版管理-获取模版列表")
    @GET
    @Path("/projects/{projectId}/templates")
    fun listTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?,
        @Parameter(description = "是否已关联到store", required = false)
        @QueryParam("storeFlag")
        storeFlag: Boolean?,
        @Parameter(description = "模版排序字段", required = false, example = "CREATE_TIME")
        @QueryParam("orderBy")
        orderBy: PTemplateOrderByType? = null,
        @Parameter(description = "orderBy排序顺序", required = false)
        @QueryParam("sort")
        sort: PTemplateSortType? = null,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = false)
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = false)
        pageSize: Int?
    ): Result<TemplateListModel>

    @Operation(summary = "创建流水线-获取模版列表")
    @GET
    @Path("/projects/{projectId}/allTemplates")
    fun listAllTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = false)
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = false)
        pageSize: Int?
    ): Result<OptionalTemplateList>

    @Operation(summary = "获取列表流水线模板")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}")
    fun getTemplate(
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
        version: Long?
    ): Result<TemplateModelDetail>

    @Operation(summary = "更新流水线模板设置")
    @PUT
    @Path("/projects/{projectId}/templates/{templateId}/settings")
    fun updateTemplateSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>

    @Operation(summary = "获取流水线模板设置")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}/settings")
    fun getTemplateSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<PipelineSetting>

    @Operation(summary = "复制流水线模板")
    @POST
    @Path("/projects/{projectId}/templates/{templateId}/copy")
    fun copyTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "复制模版请求包体", required = true)
        copyTemplateReq: CopyTemplateReq
    ): Result<TemplateId>

    @Operation(summary = "流水线另存为模版")
    @POST
    @Path("/projects/{projectId}/templates/saveAsTemplate")
    fun saveAsTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "另存为模版包体", required = true)
        saveAsTemplateReq: SaveAsTemplateReq
    ): Result<TemplateId>

    @Operation(summary = "是否有管理权限")
    @GET
    @Path("/projects/{projectId}/templates/hasManagerPermission")
    fun hasManagerPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "是否有特定模板权限")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}/hasPipelineTemplatePermission")
    fun hasPipelineTemplatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String?,
        @Parameter(description = "操作", required = true)
        @QueryParam("permission")
        permission: AuthPermission
    ): Result<Boolean>

    @Operation(summary = "是否开启模板管理权限")
    @GET
    @Path("/projects/{projectId}/templates/enableTemplatePermissionManage")
    fun enableTemplatePermissionManage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    // PAC新增
    @Operation(summary = "获取列表流水线预览")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}/preview")
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
        @QueryParam("highlightType")
        highlightType: HighlightType?
    ): Result<TemplatePreviewDetail>
}
