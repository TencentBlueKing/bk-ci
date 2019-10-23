package com.tencent.devops.openapi.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_TEMPLATE"], description = "OPEN-API-项目模板资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTemplateResource {

    @ApiOperation("获取所有种类流水线模板列表")
    @GET
    @Path("/projects/{projectId}/allTemplates")
    fun listAllTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<OptionalTemplateList>

    @ApiOperation("获取流水线模板详情")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}")
    fun getTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("模板版本", required = false)
        @QueryParam("version")
        version: Long?
    ): Result<TemplateModelDetail>

    @ApiOperation("模版管理-获取模版列表")
    @GET
    @Path("/projects/{projectId}/templates")
    fun listTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?,
        @ApiParam("是否已关联到store", required = false)
        @QueryParam("storeFlag")
        storeFlag: Boolean?
    ): Result<TemplateListModel>
}