package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.FeatureSwitchType
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
import com.tencent.devops.remotedev.pojo.record.FetchMetaDataParam
import com.tencent.devops.remotedev.pojo.record.UserWorkspaceRecordPermissionInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceLiveResolution
import com.tencent.devops.remotedev.pojo.record.WorkspaceLiveResp
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_REMOTEDEV_RECORD", description = "remotedev录屏直播相关的 service接口")
@Path("/service/remotedev_record")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemoteDevRecordResource {
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
        ip: String?,
        @Parameter(description = "是否是录屏灰度", required = true)
        @QueryParam("mediaGary")
        mediaGary: Boolean?,
        @Parameter(description = "环境ID", required = true)
        @QueryParam("envUid")
        envUid: String?
    ): Result<CheckWorkspaceRecordData>

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

    @Operation(summary = "获取工作空间直播信息")
    @GET
    @Path("/get_workspace_live_info")
    fun getWorkspaceLiveInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "分辨率(R480P,R720P,R1080P)", required = true)
        @QueryParam("resolution")
        resolution: WorkspaceLiveResolution?
    ): Result<WorkspaceLiveResp>

    @Operation(summary = "开启或关闭直播")
    @POST
    @Path("/enable_live")
    fun enableLive(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "开启或关闭录屏", required = true)
        @QueryParam("enable")
        enable: Boolean,
        @QueryParam("switchType")
        switchType: FeatureSwitchType
    ): Result<Boolean>

    @Operation(summary = "校验是否有权限查看直播")
    @GET
    @Path("/check_view_live")
    fun checkViewLive(
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
}