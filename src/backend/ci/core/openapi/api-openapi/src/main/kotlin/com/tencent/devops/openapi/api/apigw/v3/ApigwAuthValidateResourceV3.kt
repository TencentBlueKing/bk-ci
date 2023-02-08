package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
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

@Api(tags = ["OPENAPI_AUTH_V3"], description = "OPENAPI-权限相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/auth/validate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwAuthValidateResourceV3 {
    @GET
    @Path("/projects/{projectId}/isProjectUsers")
    @ApiOperation(
        "判断是否某个项目中某个组角色的成员",
        tags = ["v3_app_permission_project_check", "v3_user_permission_project_check"]
    )
    fun isProjectUser(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户Id", required = true)
        userId: String,
        @PathParam("projectId")
        @ApiParam("项目Code", required = true)
        projectId: String,
        @QueryParam("group")
        @ApiParam("用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<Boolean>
}
