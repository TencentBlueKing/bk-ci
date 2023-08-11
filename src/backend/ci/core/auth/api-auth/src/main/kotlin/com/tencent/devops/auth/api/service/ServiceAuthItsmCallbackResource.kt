package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.vo.AuthItsmCallbackInfo
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_AUTH_ITSM_CALLBACK"], description = "权限-itsm-回调")
@Path("/service/auth/itsm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthItsmCallbackResource {

    @GET
    @Path("/{projectCode}")
    @ApiOperation("获取项目审批信息")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<AuthItsmCallbackInfo?>
}
