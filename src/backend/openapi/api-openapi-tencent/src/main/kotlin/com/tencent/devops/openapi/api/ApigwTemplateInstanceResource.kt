package com.tencent.devops.openapi.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_TEMPLATE_INSTANCE"], description = "OPEN-API-实例化资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/templateInstances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTemplateInstanceResource {
    @ApiOperation("实例化流水线模板")
    @POST
    @Path("/projects/{projectId}/templates/{templateId}")
    fun createTemplateInstances(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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
}