package com.tencent.devops.auth.api

import com.tencent.devops.auth.pojo.PermissionUrlDTO
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.POST
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_RESOURCE"], description = "用户态-权限")
@Path("/user/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthResource {

    @POST
    @Path("/permissionUrl")
    @ApiOperation("权限申请重定向Url")
    fun permissionUrl(
        @ApiParam(value = "待申请实例信息")
        permissionUrlDTO: List<PermissionUrlDTO>
    ): Result<String?>
}