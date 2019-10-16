package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.third.tcls.TclsType
import com.tencent.devops.process.pojo.third.tcls.TclsEnv
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

@Api(tags = ["USER_TCLS"], description = "用户-获取 TCLS 环境列表")
@Path("/user/tcls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTclsResource {
    @ApiOperation("获取 TCLS 环境列表")
    @GET
    @Path("/projects/{projectId}/getEnvList")
    fun getEnvList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("TCLS 业务 ID", required = false)
        @QueryParam("tclsAppId")
        tclsAppId: String?,
        @ApiParam("是否是MTCLS业务", required = false)
        @QueryParam("mtclsApp")
        mtclsApp: TclsType?,
        @ApiParam("业务ServiceID", required = false)
        @QueryParam("serviceId")
        serviceId: String?,
        @ApiParam("凭证ID", required = false)
        @QueryParam("ticketId")
        ticketId: String
    ): Result<List<TclsEnv>>
}
