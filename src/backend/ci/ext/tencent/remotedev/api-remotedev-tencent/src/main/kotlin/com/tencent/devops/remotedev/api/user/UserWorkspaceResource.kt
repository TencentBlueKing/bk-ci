/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.BkTicketInfo
import com.tencent.devops.remotedev.pojo.ProjectAccessDevicePermissionsResp
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Tag(name = "USER_WORKSPACE", description = "用户-工作空间")
@Path("/{apiType:user|desktop}/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserWorkspaceResource {
    @Operation(summary = "创建新的工作空间")
    @POST
    @Path("/")
    fun createWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bkTicket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "工作空间描述", required = true)
        workspace: WorkspaceCreate
    ): Result<WorkspaceResponse>

    @Operation(summary = "获取用户工作空间列表")
    @GET
    @Path("/")
    @Deprecated("@see getWorkspaceListNew")
    fun getWorkspaceList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<Workspace>>

    @Operation(summary = "获取用户工作空间列表")
    @POST
    @Path("/search")
    fun getWorkspaceListNew(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<Workspace>>

    @Operation(summary = "获取用户工作空间列表-导出xlsx")
    @POST
    @Path("/search_export")
    fun getWorkspaceListXlsx(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?,
        search: WorkspaceSearch
    ): Response

    @Operation(summary = "删除工作空间")
    @DELETE
    @Path("/")
    fun deleteWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "开启工作空间")
    @POST
    @Path("/start")
    fun startWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bkTicket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WorkspaceResponse>

    @Operation(summary = "休眠工作空间")
    @POST
    @Path("/stop")
    fun stopWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "分享工作空间")
    @POST
    @Path("/share")
    fun shareWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "分享用户", required = true)
        @QueryParam("sharedUser")
        sharedUser: String
    ): Result<Boolean>

    @Operation(summary = "修改工作空间")
    @POST
    @Path("/edit")
    fun editWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "备注名称", required = true)
        @QueryParam("displayName")
        displayName: String
    ): Result<Boolean>

    @Operation(summary = "修改工作空间属性")
    @POST
    @Path("/modify/property")
    fun modifyWorkspaceProperty(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "备注名称", required = true)
        workspaceProperty: WorkspaceProperty
    ): Result<Boolean>

    @Operation(summary = "获取指定工作空间详情")
    @GET
    @Path("/workspace_detail")
    fun getWorkspaceDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WorkspaceDetail?>

    @Operation(summary = "获取用户工作空间详情")
    @GET
    @Path("/user_detail")
    fun getWorkspaceUserDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<WorkspaceUserDetail?>

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

    @Operation(summary = "获取用户已授权代码库列表")
    @GET
    @Path("/repository")
    fun getAuthorizedGitRepository(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模糊搜索代码库", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "git 类型", required = true)
        @QueryParam("gitType")
        @DefaultValue("GIT")
        gitType: RemoteDevGitType = RemoteDevGitType.GIT
    ): Result<List<RemoteDevRepository>>

    @Operation(summary = "获取目标授权代码库分支")
    @GET
    @Path("/repository_branch")
    fun getRepositoryBranch(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库项目全路径", required = true)
        @QueryParam("pathWithNamespace")
        pathWithNamespace: String,
        @Parameter(description = "git 类型", required = true)
        @QueryParam("gitType")
        @DefaultValue("GIT")
        gitType: RemoteDevGitType = RemoteDevGitType.GIT
    ): Result<List<String>>

    @Operation(summary = "返回目标代码库devfile路径")
    @GET
    @Path("/repository_devfile")
    fun checkDevfile(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库项目全路径", required = true)
        @QueryParam("pathWithNamespace")
        pathWithNamespace: String,
        @Parameter(description = "分支", required = true)
        @QueryParam("branch")
        branch: String,
        @Parameter(description = "git 类型", required = true)
        @QueryParam("gitType")
        @DefaultValue("GIT")
        gitType: RemoteDevGitType = RemoteDevGitType.GIT
    ): Result<List<String>>

    @Operation(summary = "根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/isOauth")
    fun isOAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @Parameter(description = "oauth认证成功后重定向到前端的地址", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?,
        @Parameter(description = "是否刷新token", required = false)
        @QueryParam("refreshToken")
        refreshToken: Boolean? = true,
        @Parameter(description = "git 类型", required = true)
        @QueryParam("gitType")
        @DefaultValue("GIT")
        gitType: RemoteDevGitType = RemoteDevGitType.GIT
    ): Result<AuthorizeResult>

    @Operation(summary = "工作空间心跳请求")
    @POST
    @Path("/heartbeat")
    fun workspaceHeartbeat(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "执行次数", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "校验用户操作工作空间权限")
    @GET
    @Path("/checkPermission")
    fun checkUserPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "校验用户是否能创建工作空间")
    @GET
    @Path("/check_user_create")
    fun checkUserCreate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>

    @Operation(summary = "更新容器的BKticket")
    @POST
    @Path("/updateBkTicket")
    fun updateBkTicket(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bkTicket信息", required = true)
        bkTicketInfo: BkTicketInfo
    ): Result<Boolean>

    @Operation(summary = "更新容器的BKticket")
    @POST
    @Path("/updateAllBkTicket")
    fun updateAllBkTicket(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bkTicket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String
    ): Result<Boolean>

    @Operation(summary = "获取指定工作空间详情")
    @GET
    @Path("/start_cloud_workspace_detail")
    fun startCloudWorkspaceDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WorkspaceStartCloudDetail?>

    @Operation(summary = "校验云桌面设备管控")
    @GET
    @Path("/project_access_device_permissions")
    fun projectAccessDevicePermissions(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "MAC地址", required = true)
        @QueryParam("macAddress")
        macAddress: String
    ): Result<Map<String, ProjectAccessDevicePermissionsResp>>
}
