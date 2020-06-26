package com.tencent.devops.auth.api

import com.tencent.devops.auth.pojo.dto.GroupDTO
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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_GROUP"], description = "权限-用户组")
@Path("/service/auth/group")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGroupResource {

    @POST
    @Path("/projectCodes/{projectCode}/")
    @ApiOperation("项目下添加指定组")
    fun createGroup(
        @ApiParam(name = "用户名", required = true)
        @PathParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(name = "项目标识", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam(name = "是否添加创建人到该分组", required = false)
        @QueryParam("addCreateUser")
        addCreateUser: Boolean?,
        @ApiParam("用户组信息", required = true)
        groupInfo: GroupDTO
    ): Result<String>
}