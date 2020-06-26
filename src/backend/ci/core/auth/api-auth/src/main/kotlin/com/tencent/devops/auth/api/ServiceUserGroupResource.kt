package com.tencent.devops.auth.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_USER_GROUP"], description = "权限-用户-用户组")
@Path("/service/auth/userGroup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceUserGroupResource {

    @POST
    @Path("/users/{userId}/groupIds/{groupId}")
    @ApiOperation("添加用户到指定组")
    fun addUser2Group(
        @ApiParam(name = "用户名", required = true)
        @PathParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(name = "用户组名称", required = true)
        @PathParam("groupId")
        groupId: String
    ): Result<Boolean>
}