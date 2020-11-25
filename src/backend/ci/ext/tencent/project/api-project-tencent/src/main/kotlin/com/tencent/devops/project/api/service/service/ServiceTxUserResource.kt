package com.tencent.devops.project.api.service.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_USER"], description = "项目-人员信息")
@Path("/service/user/tx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTxUserResource {

    @GET
    @Path("/projects/{projectCode}/roles")
    @ApiOperation("获取项目指定角色用户")
    fun getProjectUserRoles(
        @ApiParam("项目ID", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("角色Id", required = true)
        @QueryParam("roleId")
        roleId: BkAuthGroup
    ): Result<List<String>>
}