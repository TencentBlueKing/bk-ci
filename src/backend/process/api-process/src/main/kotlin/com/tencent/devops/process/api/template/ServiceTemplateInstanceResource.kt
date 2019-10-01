package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstances
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * deng
 * 2019-01-08
 */
@Api(tags = ["USER_TEMPLATE_INSTANCE"], description = "服务-流水模板-实例化资源")
@Path("/service/templateInstances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
        templateId: String
    ): Result<TemplateInstances>

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