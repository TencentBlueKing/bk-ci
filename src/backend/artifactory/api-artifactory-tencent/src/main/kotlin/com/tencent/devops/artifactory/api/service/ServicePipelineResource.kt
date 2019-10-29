package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["SERVICE_PIPELINE"], description = "版本仓库-流水线目录")
@Path("/service/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineResource {
    @ApiOperation("获取有目录权限")
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/hasPermission")
    @Path("/{projectId}/{pipelineId}/hasPermission")
    @GET
    fun hasPermission(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("路径", required = false)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("权限", required = true)
        @QueryParam("permission")
        permission: Permission
    ): Result<Boolean>
}