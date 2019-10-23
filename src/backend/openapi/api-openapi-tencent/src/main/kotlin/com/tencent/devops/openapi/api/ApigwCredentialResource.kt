package com.tencent.devops.openapi.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.enums.Permission
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

@Api(tags = ["OPEN_API_CREDENTIAL"], description = "OPEN-API-凭据资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwCredentialResource {

    @ApiOperation("获取拥有对应权限凭据列表")
    @Path("/{projectId}/hasPermissionList")
    @GET
    fun hasPermissionList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("凭证类型列表，用逗号分隔", required = false, defaultValue = "")
        @QueryParam("credentialTypes")
        credentialTypesString: String?,
        @ApiParam("对应权限", required = true, defaultValue = "")
        @QueryParam("permission")
        permission: Permission,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<Credential>>
}