package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.api.apigw.pojo.BlackListInfo
import com.tencent.devops.openapi.api.apigw.pojo.WesecResult
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OPEN_API_BUILD", description = "OPEN-API-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/auth/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwAuthResourceV3 {
    @Operation(summary = "添加用户组", tags = ["v3_app_auth_addGroup"])
    @POST
    @Path("/{projectId}/group/brach")
    fun batchCreateGroup(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户组信息", required = true)
        groupInfos: List<GroupDTO>
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectId}/resource/validate")
    @Operation(summary = "校验用户是否有action的权限", tags = ["v3_app_auth_validate"])
    fun validateUserResourcePermission(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @QueryParam("action")
        @Parameter(description = "资源类型", required = true)
        action: String,
        @PathParam("projectId")
        @Parameter(description = "项目编码", required = true)
        projectId: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源编码", required = false)
        resourceCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源编码", required = false)
        resourceType: String
    ): Result<Boolean>

    @Operation(summary = "黑名单操作", tags = ["v3_app_auth_blackList_create"])
    @POST
    @Path("/blackList/")
    fun blackListUser(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        blackList: BlackListInfo
    ): WesecResult

    @Operation(summary = "黑名单列表", tags = ["v3_app_auth_blackList_get"])
    @GET
    @Path("/blackList/")
    fun blackListUser(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?
    ): WesecResult
}
