package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
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
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_REMOTEDEV", description = "remotedev service接口")
@Path("/service/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemoteDevResource {
    @Operation(summary = "提供给START云桌面校验用户登录是否有效")
    @GET
    @Path("/ticket/validate")
    fun validateUserTicket(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "区分是否离岸外包场景", required = true)
        @QueryParam("is_offshore")
        isOffshore: Boolean,
        @Parameter(description = "登录Ticket，内网传BkTicket，离岸登录传BkToken", required = true)
        @QueryParam("ticket")
        ticket: String
    ): Result<Boolean>

    @Operation(summary = "提供给wesec获取项目下云桌面信息")
    @GET
    @Path("/project/workspace")
    fun getProjectWorkspace(
        @Parameter(description = "project_id", required = false)
        @QueryParam("project_id")
        projectId: String?,
        @Parameter(description = "ip", required = false)
        @QueryParam("ip")
        ip: String?,
        @Parameter(description = "businessLineName", required = false)
        @QueryParam("businessLineName")
        businessLineName: String?,
        @Parameter(description = "ownerName", required = false)
        @QueryParam("ownerName")
        ownerName: String?
    ): Result<List<WeSecProjectWorkspace>>

    @Operation(summary = "提供给wesec获取项目下云桌面信息")
    @GET
    @Path("/project/workspace/ip")
    fun getProjectWorkspaceIp(
        @Parameter(description = "ip", required = false)
        @QueryParam("ip")
        ip: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "提供给wesec获取创建云桌面的项目")
    @GET
    @Path("/project/list")
    fun getRemotedevProjects(
        @Parameter(description = "project_id", required = false)
        @QueryParam("project_id")
        projectId: String?
    ): Result<List<RemotedevProject>>

    @Operation(summary = "获取云研发项目的Devcloud CVM", tags = ["v4_app_remotedev_cvm", "v4_user_remotedev_cvm"])
    @GET
    @Path("/project/cvm")
    fun queryProjectRemoteDevCvm(
        @Parameter(description = "project_id", required = false)
        @QueryParam("project_id")
        projectId: String?
    ): Result<List<RemotedevCvmData>>

    @Operation(summary = "校验是否是当前项目下的云桌面")
    @GET
    @Path("/checkWorkspaceProject")
    fun checkWorkspaceProject(
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "ip", required = true)
        @QueryParam("ip")
        ip: String
    ): Result<Boolean>

    @Operation(summary = "校验当前用户是否有当前云桌面的权限")
    @GET
    @Path("/checkUserIpPermission")
    fun checkUserIpPermission(
        @Parameter(description = "user", required = true)
        @QueryParam("user")
        user: String,
        @Parameter(description = "ip", required = true)
        @QueryParam("ip")
        ip: String
    ): Result<Boolean>

    @Operation(summary = "通过已有cgsIp实例创建workspace记录")
    @POST
    @Path("/create_win_workspace_by_vm")
    fun createWinWorkspaceByVm(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "老workspace记录，可以为空，如果填写将会做清理", required = true)
        @QueryParam("oldWorkspaceName")
        oldWorkspaceName: String?,
        @Parameter(description = "项目ID，可以为空，如果填写就是团队空间，否则个人空间", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "机器uid", required = true)
        @QueryParam("uid")
        uid: String
    ): Result<Boolean>

    @Operation(summary = "提供给BCS做分配云桌面给指定用户")
    @POST
    @Path("/assignWorkspace")
    fun assignWorkspace(
        @Parameter(description = "操作人，必填", required = true)
        @QueryParam("operator")
        operator: String,
        @Parameter(description = "拥有者，为空则表示不分配，只交付项目", required = false)
        @QueryParam("owner")
        owner: String?,
        @Parameter(description = "分配数据，必填", required = true)
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean>

    @Operation(summary = "用来通知蓝盾客户端消息")
    @POST
    @Path("/notify")
    fun notifyWorkspaceInfo(
        @Parameter(description = "操作人，必填", required = true)
        @QueryParam("operator")
        operator: String,
        @Parameter(description = "通知信息", required = true)
        notifyData: WorkspaceNotifyData
    ): Result<Boolean>

    @Operation(summary = "获取windows硬件配置")
    @GET
    @Path("/resourceType/list")
    fun getWindowsResourceList(): Result<List<WindowsResourceTypeConfig>>
}
