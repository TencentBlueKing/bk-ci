package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildId
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GIT_CI_BUILD"], description = "服务-构建资源")
@Path("/service/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceBuildResource {

    @ApiOperation("重试流水线")
    @POST
    @Path("/{gitProjectId}/{buildId}/retry")
    fun retry(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("要重试的原子任务ID", required = false)
        @QueryParam("taskId")
        taskId: String? = null
    ): Result<BuildId>

    @ApiOperation("手动停止流水线")
    @DELETE
    @Path("/{gitProjectId}/{buildId}/")
    fun manualShutdown(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>
}