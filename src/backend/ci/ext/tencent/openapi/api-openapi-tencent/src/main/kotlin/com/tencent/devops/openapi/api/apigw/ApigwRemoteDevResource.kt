package com.tencent.devops.openapi.api.apigw

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.OperateCvmData
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceCloneReq
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.expert.SupRecordDataResp
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
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

    @Operation(summary = "校验token", tags = ["v4_app_ticket_check"])
    @GET
    @Path("/desktop_token_check")
    fun desktopTokenCheck(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @QueryParam("dToken")
        @Parameter(description = "dToken", required = false)
        dToken: String
    ): Result<UserOnePassword>

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
        @Parameter(description = "zoneType", required = false)
        @QueryParam("zoneType")
        zoneType: WindowsResourceZoneConfigType?,
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

    @Operation(summary = "创建windows工作空间-个人", tags = ["v4_app_remotedev_win_prosonal_create"])
    @POST
    @Path("/personal_win_workspace")
    fun createPersonalWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "zoneType", required = false)
        @QueryParam("zoneType")
        zoneType: WindowsResourceZoneConfigType?,
        @Parameter(description = "创建内容", required = true)
        data: WindowsWorkspaceCreate
    ): Result<Boolean>

    @Operation(summary = "删除windows工作空间-个人", tags = ["v4_app_remotedev_win_prosonal_delete"])
    @DELETE
    @Path("/personal_win_workspace")
    fun deletePersonalWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "获取windows工作空间-个人", tags = ["v4_app_remotedev_win_prosonal_detail"])
    @GET
    @Path("/personal_win_workspace")
    fun getPersonalWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "创建windows工作空间-项目", tags = ["v4_app_remotedev_win_project_create"])
    @POST
    @Path("/project_win_workspace")
    fun createProjectWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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

    @Operation(summary = "克隆工作空间", tags = ["v4_app_remotedev_win_workspace_clone"])
    @POST
    @Path("/workspace_clone")
    fun workspaceClone(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "workspaceName", required = false)
        @QueryParam("workspaceName")
        workspaceName: String,
        req: WorkspaceCloneReq
    ): Result<Boolean>

    @Operation(summary = "删除windows工作空间-项目", tags = ["v4_app_remotedev_win_project_delete"])
    @DELETE
    @Path("/project_win_workspace")
    fun deleteProjectWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "获取windows工作空间-项目", tags = ["v4_app_remotedev_win_project_detail"])
    @GET
    @Path("/project_win_workspace")
    fun getProjectWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "获取一个月内工作空间申请的专家协助单据", tags = ["v4_app_remotedev_expertsup_records"])
    @GET
    @Path("/expertsup/records")
    fun getExpertSupRecords(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<SupRecordDataResp>

    @Operation(summary = "获取专家协助单据", tags = ["v4_app_remotedev_expertsup_record"])
    @GET
    @Path("/expertsup/record")
    fun getExpertSupRecord(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "单据ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<SupRecordData?>

    @Operation(summary = "获取windows空闲资源数据", tags = ["v4_app_remotedev_win_quota"])
    @GET
    @Path("/get_all_windows_resource_quota")
    fun getWindowsQuota(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "获取类型", required = true)
        @QueryParam("type")
        type: QuotaType
    ): Result<Map<String, Map<String, Int>>>

    @Operation(summary = "更新项目/个人在使用云桌面上的配额", tags = ["v4_app_remotedev_usage_limit"])
    @PUT
    @Path("/update_usage_limit")
    fun updateUsageLimit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = false)
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

    @Operation(summary = "获取指定云研发项目的CVM信息列表", tags = ["v4_app_remotedev_cvm"])
    @GET
    @Path("/{projectId}/devcloud/cvmList")
    fun fetchCvmList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<Page<DevcloudCVMData>?>

    @Operation(summary = "提供给Devcloud分配云桌面拥有者或共享人", tags = ["v4_app_assign_workspace_users"])
    @POST
    @Path("/{projectId}/assign/workspace/users")
    fun assignWorkspaceUsers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "分配数据", required = true)
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean>

    @Operation(summary = "提供给Devcloud获取云桌面镜像列表", tags = ["v4_app_query_workspace_image_list"])
    @GET
    @Path("/workspace/image/list")
    fun queryWorkspaceImageList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "项目镜像id", required = true)
        @QueryParam("imageId")
        imageId: String?
    ): Result<Map<String, Any>>

    @Operation(
        summary = "提供给BCS机型置换时修改旧机器的名称，以备销毁",
        tags = ["v4_app_modify_workspace_display_name"]
    )
    @POST
    @Path("/modify/display_name")
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

    @Operation(summary = "重装云桌面系统", tags = ["v4_app_remotedev_workspace_rebuild"])
    @POST
    @Path("/workspace_rebuild")
    fun reBuildWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        rebuildReq: WorkspaceRebuildReq
    ): Result<Boolean>

    @Operation(summary = "云桌面开机", tags = ["v4_app_remotedev_workspace_start"])
    @POST
    @Path("/workspace_start")
    fun startWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "云桌面关机", tags = ["v4_app_remotedev_workspace_stop"])
    @POST
    @Path("/workspace_stop")
    fun stopWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "云桌面重启", tags = ["v4_app_remotedev_workspace_restart"])
    @POST
    @Path("/workspace_restart")
    fun restartWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "根据已存在的云桌面制作镜像", tags = ["v4_app_remotedev_image_make"])
    @POST
    @Path("/make_vm_image")
    fun makeImageByVm(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean>

    @Operation(summary = "修改工作空间属性", tags = ["v4_app_remotedev_modify_property"])
    @POST
    @Path("/modify_property")
    fun modifyWorkspaceProperty(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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

    @Operation(summary = "删除项目的自定义镜像", tags = ["v4_app_remotedev_delete_project_image"])
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

    @Operation(summary = "增删CVM机器回调", tags = ["v4_app_remotedev_operate_cvm_callback"])
    @POST
    @Path("/operate_cvm_callback")
    fun operateCvmCallback(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        data: OperateCvmData
    ): Result<Boolean>

    @Operation(summary = "开启或关闭工作空间录屏", tags = ["v4_app_enable_workspace_record"])
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

    @Operation(summary = "检查是否开启录屏并获取推流地址", tags = ["v4_app_check_workspace_record_enable_address"])
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

    @Operation(
        summary = "检查用户是否有查看当前工作空间录像的权限",
        tags = ["v4_app_check_user_view_workspace_record_permission"]
    )
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

    @Operation(summary = "磁盘扩容", tags = ["v4_app_remotedev_workspace_expand_disk"])
    @POST
    @Path("/workspace_expand_disk")
    fun expandWorkspaceDisk(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        @QueryParam("size")
        size: String
    ): Result<ExpandDiskValidateResp?>

    @Operation(summary = "云桌面调整配置", tags = ["v4_app_remotedev_workspace_upgrade"])
    @POST
    @Path("/workspace/upgrade")
    fun upgradeWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        upgradeReq: WorkspaceUpgradeReq
    ): Result<Boolean>

    @Operation(summary = "剔除当前用户所有云桌面相关权限", tags = ["v4_app_remotedev_remove_user_permission"])
    @POST
    @Path("/remove_user_permission")
    fun removeUserPermission(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "被移除用户", required = true)
        @QueryParam("removeUser")
        removeUser: String
    ): Result<Boolean>
}
