package com.tencent.devops.openapi.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineTemplate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_TEMPLATE_PIPELINE"], description = "OPEN-API-项目流水线模板模板资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/template/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwPipelineTemplateResource {

    @ApiOperation("获取项目的模板列表")
    @GET
    @Path("/projects/{projectId}")
    fun listTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Map<String, PipelineTemplate>>
}