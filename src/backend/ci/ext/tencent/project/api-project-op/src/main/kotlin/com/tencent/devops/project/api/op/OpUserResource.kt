package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PROJECT_USER"], description = "OP_用户")
@Path("/op/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpUserResource {

    @ApiOperation("同步tof组织信息")
    @PUT
    @Path("/{userId}")
    fun refreshUserGroup(
        @ApiParam(value = "用户id", required = true)
        @PathParam("userId")
        userId: String
    ): Result<UserDeptDetail?>
}