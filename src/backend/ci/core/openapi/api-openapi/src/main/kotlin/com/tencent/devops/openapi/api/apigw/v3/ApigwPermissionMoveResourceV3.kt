package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineIdInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_MOVE"], description = "OPEN-API-迁移")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/permission/move")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwPermissionMoveResourceV3 {

    @ApiOperation("获取项目下pipelineId+自增id", tags = ["v3_app_pipeline_id_info", "v3_user_pipeline_id_info"])
    @GET
    @Path("/projects/{projectId}/pipelineIds/list")
    fun getProjectPipelineIds(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目Code", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineIdInfo>>

    @ApiOperation("关联iam项目", tags = ["v3_app_relation_iam"])
    @PUT
    @Path("/projects/{projectId}/relationProject")
    fun relationProject(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目Code", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("iam分级管理员ID", required = true)
        @QueryParam("relationId")
        relationId: String
    ): com.tencent.devops.project.pojo.Result<Boolean>
}
