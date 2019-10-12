package com.tencent.devops.project.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.OrganizationType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PROJECT_ORGANIZATION"], description = "蓝盾项目列表组织架构接口")
@Path("/user/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectOrganizationResource {

    @GET
    @Path("/types/{type}/ids/{id}")
    fun getOrganizations(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("bg, 部门或者中心")
        @PathParam("type")
        type: OrganizationType,
        @ApiParam("ID")
        @PathParam("id")
        id: Int
    ): Result<List<OrganizationInfo>>
}