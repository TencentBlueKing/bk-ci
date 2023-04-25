package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.api.apigw.pojo.BlackListInfo
import com.tencent.devops.openapi.api.apigw.pojo.WesecResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_BUILD"], description = "OPEN-API-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/auth/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwAuthResourceV3 {
    @ApiOperation("添加用户组")
    @POST
    @Path("/{projectId}/group/brach")
    fun batchCreateGroup(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(name = "项目标识", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组信息", required = true)
        groupInfos: List<GroupDTO>
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectId}/resource/validate")
    @ApiOperation("校验用户是否有action的权限")
    fun validateUserResourcePermission(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @QueryParam("action")
        @ApiParam("资源类型", required = true)
        action: String,
        @PathParam("projectId")
        @ApiParam("项目编码", required = true)
        projectId: String,
        @QueryParam("resourceCode")
        @ApiParam("资源编码", required = false)
        resourceCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源编码", required = false)
        resourceType: String
    ): Result<Boolean>

    @ApiOperation("黑名单操作")
    @POST
    @Path("/blackList/")
    fun blackListUser(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        blackList: BlackListInfo
    ): WesecResult

    @ApiOperation("黑名单列表")
    @GET
    @Path("/blackList/")
    fun blackListUser(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?
    ): WesecResult
}
