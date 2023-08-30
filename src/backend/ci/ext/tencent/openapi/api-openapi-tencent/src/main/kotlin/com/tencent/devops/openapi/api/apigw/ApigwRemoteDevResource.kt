package com.tencent.devops.openapi.api.apigw

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
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

@Api(tags = ["OPEN_API_REMOTEDEV"], description = "OPEN-API-REMOTEDEV服务")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwRemoteDevResource {
    @ApiOperation("提供给START云桌面校验用户登录是否有效", tags = ["v4_app_ticket_validate", "v4_user_ticket_validate"])
    @GET
    @Path("/ticket/validate")
    fun validateUserTicket(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("区分是否离岸外包场景", required = true)
        @QueryParam("is_offshore")
        isOffshore: Boolean,
        @ApiParam("登录Ticket，内网传BkTicket，离岸登录传BkToken", required = true)
        @QueryParam("ticket")
        ticket: String
    ): Result<Boolean>

    @ApiOperation("提供给wesec获取云桌面信息", tags = ["v4_app_project_workspace", "v4_user_project_workspace"])
    @GET
    @Path("/project/workspace")
    fun queryProjectWorkspace(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID", required = false)
        @QueryParam("project_id")
        projectId: String?
    ): Result<List<WeSecProjectWorkspace>>

    @ApiOperation("提供给wesec获取云桌面信息", tags = ["v4_app_remotedev_project_list", "v4_user_remotedev_project_list"])
    @GET
    @Path("/project/list")
    fun queryWorkspaceProjects(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?
    ): Result<List<RemotedevProject>>
}
