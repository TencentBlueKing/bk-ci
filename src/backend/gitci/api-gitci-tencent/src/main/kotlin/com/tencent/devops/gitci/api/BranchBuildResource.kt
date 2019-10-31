package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.BranchBuildHistory
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.GET
import javax.ws.rs.QueryParam
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GIT_CI_BRANCH"], description = "Branches页面")
@Path("/service/branch/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BranchBuildResource {

    @ApiOperation("按分支查最近5次构建列表")
    @GET
    @Path("/list/{gitProjectId}")
    fun getBranchBuildList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "defaultBranch", required = false)
        @QueryParam("defaultBranch")
        defaultBranch: String?
    ): Result<List<BranchBuildHistory>>
}