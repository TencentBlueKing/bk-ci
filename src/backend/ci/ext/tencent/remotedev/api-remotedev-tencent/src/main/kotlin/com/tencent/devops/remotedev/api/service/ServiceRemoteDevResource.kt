package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.OperateCvmData
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
import com.tencent.devops.remotedev.pojo.windows.QuotaInApiRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
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

    @Operation(summary = "校验token")
    @GET
    @Path("/desktop_token_check")
    fun desktopTokenCheck(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @QueryParam("dToken")
        @Parameter(description = "dToken", required = false)
        dToken: String
    ): Result<UserOnePassword>

    @Operation(summary = "提供给wesec获取项目下批量云桌面信息")
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
        @Parameter(description = "项目ID，可以为空，如果oldWorkspaceName=null 必填", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "工作空间类型，可以为空，如果oldWorkspaceName=null 必填", required = true)
        @QueryParam("ownerType")
        ownerType: WorkspaceOwnerType?,
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
        @Parameter(description = "zoneType", required = false)
        @QueryParam("zoneType")
        zoneType: WindowsResourceZoneConfigType?,
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

    @Operation(summary = "用来通知云桌面消息, 附带发送机器IP校验")
    @POST
    @Path("/notify/desktop")
    fun notifyDesktopCheckIp(
        @Parameter(description = "发送机器IP，必填", required = true)
        @QueryParam("ip")
        ip: String,
        @Parameter(description = "通知信息", required = true)
        notifyData: WorkspaceDesktopNotifyData
    ): Result<Boolean>

    @Operation(summary = "获取windows硬件配置")
    @GET
    @Path("/resourceType/list")
    fun getWindowsResourceList(): Result<List<WindowsResourceTypeConfig>>

    @Operation(summary = "创建windows工作空间")
    @POST
    @Path("/personal_win_workspace")
    fun createPersonalWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "zoneType", required = false)
        @QueryParam("zoneType")
        zoneType: WindowsResourceZoneConfigType?,
        @Parameter(description = "创建内容", required = true)
        data: WindowsWorkspaceCreate
    ): Result<Boolean>

    @Operation(summary = "删除windows工作空间")
    @DELETE
    @Path("/personal_win_workspace")
    fun deletePersonalWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "获取windows工作空间")
    @GET
    @Path("/personal_win_workspace")
    fun getPersonalWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "创建windows工作空间-项目")
    @POST
    @Path("/project_win_workspace")
    fun createProjectWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "zoneType", required = false)
        @QueryParam("zoneType")
        zoneType: WindowsResourceZoneConfigType?,
        @Parameter(description = "创建内容", required = true)
        data: WindowsWorkspaceCreate
    ): Result<Boolean>

    @Operation(summary = "删除windows工作空间-项目")
    @DELETE
    @Path("/project_win_workspace")
    fun deleteProjectWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "获取windows工作空间-项目")
    @GET
    @Path("/project_win_workspace")
    fun getProjectWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "获取专家求助单据数据")
    @GET
    @Path("/fetch_expert_sup_record")
    fun fetchExpertSupRecord(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "从什么时间起的数据", required = true)
        @QueryParam("createLaterTime")
        createLaterTimestamp: Long
    ): Result<List<SupRecordData>>

    @Operation(summary = "获取windows空闲资源数据")
    @GET
    @Path("/get_all_windows_resource_quota")
    fun getWindowsQuota(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "获取类型", required = true)
        @QueryParam("type")
        type: QuotaType
    ): Result<Map<String, Map<String, Int>>>

    @Operation(summary = "更新项目/个人在使用云桌面上的配额")
    @PUT
    @Path("/update_usage_limit")
    fun updateUsageLimit(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "机型", required = false)
        @QueryParam("machineType")
        machineType: String?,
        @Parameter(description = "配额增量(可负，可零，可正)", required = true)
        @QueryParam("count")
        count: Int,
        @Parameter(description = "返回可用配额", required = false)
        @QueryParam("available")
        available: Boolean?
    ): Result<QuotaInApiRes>

    @Operation(summary = "获取DevcloudCvm列表")
    @GET
    @Path("/devcloud/cvmList")
    fun fetchCvmList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<Page<DevcloudCVMData>?>

    @Operation(summary = "分配云桌面拥有者和共享人")
    @POST
    @Path("/assign_user")
    fun assignUser(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "工作空间描述", required = true)
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean>

    @Operation(summary = "获取镜像列表")
    @GET
    @Path("/image/list")
    fun getWorkspaceImageList(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Map<String, Any>>

    @Operation(summary = "修改工作空间别名")
    @POST
    @Path("/modify/display_name")
    @Deprecated("不要新增功能，希望废弃该接口")
    fun modifyWorkspaceDisplayName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "实例IP", required = true)
        @QueryParam("ip")
        ip: String,
        @Parameter(description = "别名", required = true)
        @QueryParam("displayName")
        displayName: String
    ): Result<Boolean>

    @Operation(summary = "重装云桌面系统")
    @POST
    @Path("/workspace_rebuild")
    fun reBuildWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        rebuildReq: WorkspaceRebuildReq
    ): Result<Boolean>

    @Operation(summary = "云桌面开机")
    @POST
    @Path("/workspace_start")
    fun startWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "云桌面关机")
    @POST
    @Path("/workspace_stop")
    fun stopWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "云桌面重启")
    @POST
    @Path("/workspace_restart")
    fun restartWorkspace(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "根据已存在的云桌面制作镜像")
    @POST
    @Path("/make_vm_image")
    fun makeImageByVm(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean>

    @Operation(summary = "云桌面SDK获取应用token", tags = ["v4_app_desktop_sdk_token"])
    @POST
    @Path("/token")
    fun getToken(
        @Parameter(description = "IP", required = false)
        @QueryParam("desktopIP")
        desktopIP: String,
        sign: DesktopTokenSign
    ): Result<String>

    @Operation(summary = "修改工作空间属性")
    @POST
    @Path("/modify_property")
    fun modifyWorkspaceProperty(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "实例IP", required = true)
        @QueryParam("ip")
        ip: String?,
        @Parameter(description = "备注名称", required = true)
        workspaceProperty: WorkspaceProperty
    ): Result<Boolean>

    @Operation(summary = "工作空间扩展硬盘回调")
    @POST
    @Path("/workspace_expand_disk_callback")
    fun workspaceExpandDiskCallback(
        @QueryParam("taskId")
        taskId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @QueryParam("operator")
        operator: String
    )

    @Operation(summary = "删除工作空间镜像")
    @DELETE
    @Path("/delete/image")
    fun deleteProjectImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "镜像ID", required = true)
        @QueryParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "增删cvm回调")
    @POST
    @Path("/op_cvm_callback")
    fun opCvm(
        data: OperateCvmData
    ): Result<Boolean>
}
