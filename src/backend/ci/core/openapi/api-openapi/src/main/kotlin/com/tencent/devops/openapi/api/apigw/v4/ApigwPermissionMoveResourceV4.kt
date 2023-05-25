package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
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

@Api(tags = ["OPEN_API_MOVE_V4"], description = "OPEN-API-迁移")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/permission/move/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwPermissionMoveResourceV4 {

    @ApiOperation("获取项目下pipelineId+自增id", tags = ["v4_app_pipeline_id_info", "v4_user_pipeline_id_info"])
    @GET
    @Path("/pipeline_id_list")
    fun getProjectPipelineIds(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @ApiParam("项目Code", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineIdInfo>>

    @ApiOperation("关联iam项目", tags = ["v4_app_relation_iam"])
    @PUT
    @Path("/relation_project")
    fun relationProject(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @ApiParam("项目Code", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("iam分级管理员ID", required = true)
        @QueryParam("relationId")
        relationId: String
    ): Result<Boolean>
}
