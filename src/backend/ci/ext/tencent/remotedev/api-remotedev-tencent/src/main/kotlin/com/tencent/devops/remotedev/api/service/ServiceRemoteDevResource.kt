package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.OperateCvmData
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceCloneReq
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.image.DeleteImageResp
import com.tencent.devops.remotedev.pojo.image.ListImagesData
import com.tencent.devops.remotedev.pojo.image.ListImagesResp
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.itsm.BKItsmCreateTicketReq
import com.tencent.devops.remotedev.pojo.itsm.BKItsmCreateTicketRespData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.EnableRemotedevData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.RemotedevProjectNew
import com.tencent.devops.remotedev.pojo.project.UpdateRemotedevDataManagers
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
import com.tencent.devops.remotedev.pojo.record.FetchMetaDataParam
import com.tencent.devops.remotedev.pojo.record.UserWorkspaceRecordPermissionInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.pojo.remotedev.TaskResp
import com.tencent.devops.remotedev.pojo.remotedev.VmDiskInfo
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
import com.tencent.devops.remotedev.pojo.windows.QuotaInApiRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

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

    @Operation(summary = "获取开启云桌面的项目列表")
    @GET
    @Path("/project/list/new")
    fun getRemotedevProjectsNew(
        @Parameter(description = "project_id", required = false)
        @QueryParam("project_id")
        projectId: String?,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<List<RemotedevProjectNew>>

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

    @Operation(summary = "克隆windows工作空间")
    @POST
    @Path("/workspace_clone")
    fun workspaceClone(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "workspaceName", required = false)
        @QueryParam("workspaceName")
        workspaceName: String,
        req: WorkspaceCloneReq
    ): Result<Boolean>

    @Operation(summary = "克隆windows工作空间，返回任务ID")
    @POST
    @Path("/workspace_clone_task")
    fun workspaceCloneTask(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "workspaceName", required = false)
        @QueryParam("workspaceName")
        workspaceName: String,
        req: WorkspaceCloneReq
    ): Result<TaskResp>

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

    @Deprecated("未来fetch_expert_sup_record_any使用会把这个接口废弃")
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

    @Operation(summary = "获取某条专家求助单据数据")
    @GET
    @Path("/fetch_expert_sup_record_any")
    fun fetchExpertSupRecordAny(
        @Parameter(description = "单据ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<SupRecordData?>

    @Operation(summary = "获取windows空闲资源数据")
    @GET
    @Path("/get_all_windows_resource_quota")
    fun getWindowsQuota(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "获取类型", required = true)
        @QueryParam("type")
        // todo 待废弃
        type: QuotaType?,
        @Parameter(description = "地域类型", required = true)
        @QueryParam("zoneType")
        zoneType: WindowsResourceZoneConfigType
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

    @Deprecated(message = "老的下掉，要被新的代替")
    @Operation(summary = "获取镜像列表")
    @GET
    @Path("/image/list")
    fun getWorkspaceImageList(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "项目镜像id", required = true)
        @QueryParam("imageId")
        imageId: String?
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

    @Operation(summary = "开启或关闭工作空间录屏")
    @PUT
    @Path("/enable_workspace_record")
    fun enableWorkspaceRecord(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "开启或关闭录屏", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @Operation(summary = "检查是否开启录屏并获取推流地址")
    @GET
    @Path("/check_workspace_record_enable_address")
    fun checkWorkspaceEnableAddress(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "appId", required = true)
        @QueryParam("appId")
        appId: Long,
        @Parameter(description = "实例IP", required = true)
        @QueryParam("ip")
        ip: String
    ): Result<CheckWorkspaceRecordData>

    @Deprecated("有了token后这个方法可能不会再使用，观察下如果不使用可以废弃")
    @Operation(summary = "检查用户是否有产看当前工作空间录像的权限")
    @GET
    @Path("/check_user_view_workspace_record_permission")
    fun checkUserViewWorkspacePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "扩容磁盘大小")
    @POST
    @Path("/expanddisk")
    fun expandDisk(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @QueryParam("size")
        size: String,
        @QueryParam("pvcId")
        pvcId: String?
    ): Result<ExpandDiskValidateResp?>

    @Operation(summary = "创建磁盘")
    @POST
    @Path("/createdisk")
    fun createDisk(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @QueryParam("size")
        size: String
    ): Result<CreateDiskResp>

    @Operation(summary = "获取磁盘列表")
    @GET
    @Path("/disklist")
    fun fetchDiskList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<List<VmDiskInfo>>

    @Operation(summary = "云桌面调整配置")
    @POST
    @Path("/workspace/{workspaceName}/upgrade")
    fun upgradeWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        upgradeReq: WorkspaceUpgradeReq
    ): Result<Boolean>

    @Operation(summary = "剔除当前用户所有云桌面相关权限")
    @POST
    @Path("/remove_user_permission")
    fun removeUserPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "被移除用户", required = true)
        @QueryParam("removeUser")
        removeUser: String
    ): Result<Boolean>

    @Operation(summary = "重新装载云桌面环境hook配置")
    @POST
    @Path("/reload_env_hook")
    fun reloadEnvHook(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "环境env hash id", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 hash id, 为null时将对环境下所有节点重载hook", required = true)
        @QueryParam("nodeHashIds")
        nodeHashIds: List<String>?
    )

    @Operation(summary = "重新装载云桌面环境hook配置")
    @POST
    @Path("/delete_env_hook")
    fun deleteEnvHook(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "环境env hash id", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 hash id, 为null时将对环境下所有节点重载hook", required = true)
        @QueryParam("nodeHashIds")
        nodeHashIds: List<String>?
    )

    @Operation(summary = "获取用户工作空间列表")
    @POST
    @Path("/workspaces_search")
    fun getWorkspaceListNew(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<ProjectWorkspace>>

    @Operation(summary = "查询录屏权限相关信息")
    @GET
    @Path("/get_user_workspace_record_permission_info")
    fun getUserWorkspaceRecordPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<UserWorkspaceRecordPermissionInfo>

    @Operation(summary = "录屏权限续期")
    @POST
    @Path("/update_user_workspace_record_permission_info")
    fun updateUserWorkspaceRecordPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "查看当前工作空间录屏元数据")
    @POST
    @Path("/get_user_workspace_record_metadata")
    fun getViewRecordMetadata(
        data: FetchMetaDataParam
    ): Result<Page<WorkspaceRecordMetadata>>

    @Operation(summary = "获取指定工作空间详情时间线")
    @GET
    @Path("/detail_timeline")
    fun getWorkspaceTimeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<WorkspaceOpHistory>>

    @Operation(summary = "获取工作空间录屏密钥")
    @GET
    @Path("/get_workspace_record_ticket")
    fun getWorkspaceRecordTicket(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "skToken", required = true)
        @QueryParam("token")
        token: String
    ): Result<String>

    @Operation(summary = "查询任务状态")
    @GET
    @Path("/get_task_status")
    fun getTaskStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "taskId", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<WorkspaceTaskStatus?>

    @Operation(summary = "蓝盾项目开启或关闭云研发")
    @POST
    @Path("/enable_project_remotedev")
    fun enableProjectRemotedev(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: EnableRemotedevData
    ): Result<Boolean>

    @Operation(summary = "修改项目云研发管理员")
    @POST
    @Path("/update_project_remotedev_managers")
    fun updateProjectRemotedevManager(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: UpdateRemotedevDataManagers
    ): Result<Boolean>

    @Operation(summary = "获取镜像列表")
    @POST
    @Path("/images")
    fun fetchImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: ListImagesData
    ): Result<ListImagesResp?>

    @Operation(summary = "删除镜像")
    @DELETE
    @Path("/delete_image")
    fun deleteImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "镜像 ID", required = true)
        @QueryParam("imageId")
        imageId: String,
        @Parameter(description = "延迟删除时间，单位秒", required = false)
        @QueryParam("delaySeconds")
        delaySeconds: Int?
    ): Result<DeleteImageResp>

    @Operation(summary = "创建ITSM单据流程")
    @POST
    @Path("/create_itsm_ticket")
    fun createItsmTicket(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "请求参数", required = true)
        createReq: BKItsmCreateTicketReq
    ): Result<BKItsmCreateTicketRespData>
}
