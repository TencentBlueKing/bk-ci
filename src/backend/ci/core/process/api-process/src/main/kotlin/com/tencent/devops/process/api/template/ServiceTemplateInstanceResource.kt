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

package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_TEMPLATE_INSTANCE"], description = "服务-流水模板-实例化资源")
@Path("/service/templateInstances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceTemplateInstanceResource {

    @ApiOperation("创建流水线模板")
    @POST
    @Path("/projects/{projectId}/templates/{templateId}")
    fun createTemplateInstances(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("模板版本", required = true)
        @QueryParam("version")
        version: Long,
        @ApiParam("是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @ApiParam("创建实例", required = true)
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet

    @ApiOperation("批量更新流水线模板实例")
    @PUT
    @Path("/projects/{projectId}/templates/{templateId}")
    fun updateTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("版本名", required = true)
        @QueryParam("version")
        version: Long,
        @ApiParam("是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @ApiParam("模板实例", required = true)
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet

    @ApiOperation("批量更新流水线模板实例")
    @PUT
    @Path("/projects/{projectId}/templates/{templateId}/updateTemplate")
    fun updateTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("版本名", required = true)
        @QueryParam("versionName")
        versionName: String,
        @ApiParam("是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @ApiParam("模板实例", required = true)
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet

    @ApiOperation("通过流水线ID获取流水线启动参数")
    @POST
    @Path("/projects/{projectId}/templates/{templateId}/pipelines")
    fun listTemplateInstancesParams(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("模板版本", required = true)
        @QueryParam("version")
        version: Long,
        @ApiParam("创建实例", required = true)
        pipelineIds: List<PipelineId>
    ): Result<Map<String/*pipelineId*/, TemplateInstanceParams>>

    @ApiOperation("列表流水线模板实例")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}")
    fun listTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam("每页多少条", required = false, defaultValue = "30")
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam("名字搜索的关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @ApiParam("排序字段", required = false)
        @QueryParam("sortType")
        sortType: TemplateSortTypeEnum?,
        @ApiParam("是否降序", required = false)
        @QueryParam("desc")
        desc: Boolean?
    ): Result<TemplateInstancePage>

    @ApiOperation("查询流水线模板实例总数")
    @POST
    @Path("/projects/{projectId}/countTemplateInstance")
    fun countTemplateInstance(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        templateIds: Collection<String>
    ): Result<Int>

    @ApiOperation("查询流水线模板实例总数")
    @POST
    @Path("/projects/{projectId}/countTemplateInstanceDetail")
    fun countTemplateInstanceDetail(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        templateIds: Collection<String>
    ): Result<Map<String, Int>>
}
