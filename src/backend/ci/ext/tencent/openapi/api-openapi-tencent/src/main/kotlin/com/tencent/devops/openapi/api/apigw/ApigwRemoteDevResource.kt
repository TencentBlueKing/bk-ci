package com.tencent.devops.openapi.api.apigw

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OPEN_API_REMOTEDEV", description = "OPEN-API-REMOTEDEV服务")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwRemoteDevResource {

    @Operation(summary = "提供给START云桌面校验用户登录是否有效", tags = ["v4_app_ticket_validate"])
    @GET
    @Path("/ticket/validate")
    fun validateUserTicket(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "区分是否离岸外包场景", required = true)
        @QueryParam("is_offshore")
        isOffshore: Boolean,
        @Parameter(description = "登录Ticket，内网传BkTicket，离岸登录传BkToken", required = true)
        @QueryParam("ticket")
        ticket: String
    ): Result<Boolean>

    @Operation(summary = "提供给wesec获取云桌面信息", tags = ["v4_app_project_workspace"])
    @GET
    @Path("/project/workspace")
    fun queryProjectWorkspace(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("project_id")
        projectId: String?,
        @Parameter(description = "ip", required = false)
        @QueryParam("ip")
        ip: String?
    ): Result<List<WeSecProjectWorkspace>>

    @Operation(summary = "提供给wesec获取云桌面信息", tags = ["v4_app_remotedev_project_list"])
    @GET
    @Path("/project/list")
    fun queryWorkspaceProjects(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("project_id")
        projectId: String?
    ): Result<List<RemotedevProject>>

    @Operation(summary = "获取云研发项目的Devcloud CVM", tags = ["v4_app_remotedev_cvm"])
    @GET
    @Path("/project/cvm")
    fun queryProjectRemoteDevCvm(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("project_id")
        projectId: String?
    ): Result<List<RemotedevCvmData>>

    @Operation(summary = "提供给套件部署校验用户和云桌面是否有权限", tags = ["v4_app_check_cgs_permission"])
    @GET
    @Path("/check/cgs/permission")
    fun checkUserCgsPermission(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "云桌面IP", required = true)
        @QueryParam("ip")
        ip: String
    ): Result<Boolean>

    @Operation(summary = "提供给BCS做分配云桌面给指定项目或用户", tags = ["v4_app_assign_workspace"])
    @POST
    @Path("/assign/workspace")
    fun assignWorkspace(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人，必填", required = true)
        @QueryParam("operator")
        operator: String,
        @Parameter(description = "拥有者，为空则表示不分配，只交付项目", required = false)
        @QueryParam("owner")
        owner: String?,
        @Parameter(description = "分配数据，必填", required = true)
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean>

    @Operation(summary = "指定项目获取云桌面信息", tags = ["v4_app_list_workspaces_with_projectId"])
    @GET
    @Path("/{projectId}/workspaces")
    fun listWorkspacesWithProjectId(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "云桌面IP", required = false)
        @QueryParam("ip")
        ip: String?
    ): Result<List<WeSecProjectWorkspace>>

    @Operation(summary = "用来通知蓝盾客户端消息", tags = ["v4_app_workspace_notify"])
    @POST
    @Path("/workspace/notify")
    fun notifyWorkspaceInfo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人，必填", required = true)
        @QueryParam("operator")
        operator: String,
        @Parameter(description = "通知信息", required = true)
        notifyData: WorkspaceNotifyData
    ): Result<Boolean>

    @Operation(summary = "校验是否是当前项目下的云桌面", tags = ["v4_app_check_project_workspace"])
    @GET
    @Path("/checkWorkspaceProject")
    fun checkWorkspaceProject(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "ip", required = true)
        @QueryParam("ip")
        ip: String
    ): Result<Boolean>

    @Operation(summary = "提供给Devcloud获取硬件资源配置", tags = ["v4_app_resourceType_list"])
    @GET
    @Path("/resourceType/list")
    fun getWindowsResourceList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?
    ): Result<List<WindowsResourceTypeConfig>>

    @Operation(summary = "提供获取云桌面信息", tags = ["v4_user_sg_project_workspace"])
    @GET
    @Path("/project/workspace_sg")
    fun querySGProjectWorkspace(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "ip", required = true)
        @QueryParam("taiUser")
        taiUser: String
    ): Result<List<WeSecProjectWorkspace>>
}
