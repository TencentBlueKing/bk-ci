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

package com.tencent.devops.remotedev.resources.user

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserWorkspaceResource
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
import com.tencent.devops.remotedev.pojo.tai.Moa2faReqData
import com.tencent.devops.remotedev.pojo.tai.Moa2faRespData
import com.tencent.devops.remotedev.pojo.tai.Moa2faVerifyReqData
import com.tencent.devops.remotedev.pojo.tai.Moa2faVerifyRespData
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.RepositoryService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.StartControl
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserWorkspaceResourceImpl @Autowired constructor(
    private val gitTransfer: RemoteDevGitTransfer,
    private val workspaceService: WorkspaceService,
    private val permissionService: PermissionService,
    private val repositoryService: RepositoryService,
    private val createControl: CreateControl,
    private val startControl: StartControl,
    private val sleepControl: SleepControl,
    private val deleteControl: DeleteControl
) : UserWorkspaceResource {

    @AuditEntry(actionId = ActionId.CGS_CREATE)
    override fun createWorkspace(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspace: WorkspaceCreate
    ): Result<WorkspaceResponse> {
        return Result(createControl.createWorkspace(userId, bkTicket, projectId, workspace))
    }

    @AuditEntry(actionId = ActionId.CGS_START)
    override fun startWorkspace(
        userId: String,
        bkTicket: String,
        workspaceName: String
    ): Result<WorkspaceResponse> {
        return Result(startControl.startWorkspace(userId, workspaceName))
    }

    @AuditEntry(actionId = ActionId.CGS_STOP)
    override fun stopWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(sleepControl.stopWorkspace(userId, workspaceName))
    }

    @AuditEntry(actionId = ActionId.CGS_SHARE)
    override fun shareWorkspace(userId: String, workspaceName: String, sharedUser: String): Result<Boolean> {
        return Result(workspaceService.shareWorkspace(userId, workspaceName, sharedUser))
    }

    @AuditEntry(actionId = ActionId.CGS_EDIT)
    override fun editWorkspace(userId: String, workspaceName: String, displayName: String): Result<Boolean> {
        return Result(
            workspaceService.modifyWorkspaceProperty(
                userId = userId,
                workspaceName = workspaceName,
                ip = null,
                workspaceProperty = WorkspaceProperty(displayName)
            )
        )
    }

    override fun modifyWorkspaceProperty(
        userId: String,
        workspaceName: String,
        workspaceProperty: WorkspaceProperty
    ): Result<Boolean> {
        return Result(
            workspaceService.modifyWorkspaceProperty(
                userId = userId,
                workspaceName = workspaceName,
                ip = null,
                workspaceProperty = workspaceProperty
            )
        )
    }

    @AuditEntry(actionId = ActionId.CGS_DELETE)
    override fun deleteWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(deleteControl.deleteWorkspace(userId, workspaceName))
    }

    override fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Result<Page<Workspace>> {
        return Result(workspaceService.getWorkspaceList(userId, page, pageSize, null))
    }

    override fun getWorkspaceListNew(
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<Workspace>> {
        return Result(workspaceService.getWorkspaceList(userId, page, pageSize, search))
    }

    @AuditEntry(actionId = ActionId.CGS_VIEW)
    override fun getWorkspaceDetail(userId: String, workspaceName: String): Result<WorkspaceDetail?> {
        return Result(workspaceService.getWorkspaceDetail(userId, workspaceName))
    }

    override fun getWorkspaceUserDetail(userId: String): Result<WorkspaceUserDetail?> {
        return Result(workspaceService.getWorkspaceUserDetail(userId))
    }

    override fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        gitType: RemoteDevGitType
    ): Result<List<RemoteDevRepository>> {
        return Result(
            repositoryService.getAuthorizedGitRepository(
                userId = userId,
                search = search,
                page = page,
                pageSize = pageSize,
                gitType = gitType
            )
        )
    }

    override fun getRepositoryBranch(
        userId: String,
        pathWithNamespace: String,
        gitType: RemoteDevGitType
    ): Result<List<String>> {
        return Result(
            repositoryService.getRepositoryBranch(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                gitType = gitType
            )
        )
    }

    override fun getWorkspaceTimeline(
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WorkspaceOpHistory>> {
        return Result(
            workspaceService.getWorkspaceTimeline(
                userId = userId,
                workspaceName = workspaceName,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun checkDevfile(
        userId: String,
        pathWithNamespace: String,
        branch: String,
        gitType: RemoteDevGitType
    ): Result<List<String>> {
        return Result(
            workspaceService.checkDevfile(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                branch = branch,
                gitType = gitType
            )
        )
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        refreshToken: Boolean?,
        gitType: RemoteDevGitType
    ): Result<AuthorizeResult> {
        // 权限校验？
        return gitTransfer.load(gitType).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            refreshToken = refreshToken
        )
    }

    override fun checkUserPermission(userId: String, workspaceName: String): Result<Boolean> {
        return Result(permissionService.checkUserPermission(userId, workspaceName))
    }

    override fun checkUserCreate(userId: String): Result<Boolean> {
        return Result(permissionService.checkUserCreate(userId))
    }

    @AuditEntry(actionId = ActionId.CGS_VIEW)
    override fun startCloudWorkspaceDetail(userId: String, workspaceName: String): Result<WorkspaceStartCloudDetail?> {
        return Result(workspaceService.startCloudWorkspaceDetail(userId, workspaceName))
    }

    override fun projectAccessDevicePermissions(
        userId: String,
        macAddress: String
    ): Result<Map<String, ProjectAccessDevicePermissionsResp>> {
        return Result(workspaceService.projectAccessDevicePermissions(userId, macAddress))
    }

    override fun checkMoa2fa(userId: String, workspaceName: String): Result<Boolean> {
        return Result(workspaceService.checkMoa2fa(userId, workspaceName))
    }

    override fun createMoa2faRequest(userId: String, moa2faReqData: Moa2faReqData): Result<Moa2faRespData> {
        return Result(workspaceService.createMoa2faRequest(userId = userId, moa2faReqData = moa2faReqData))
    }

    override fun verifyMoa2faResult(userId: String, moa2faVerifyReqData: Moa2faVerifyReqData): Result<Moa2faVerifyRespData> {
        return Result(workspaceService.verifyMoa2faResult(userId = userId, moa2faVerifyReqData = moa2faVerifyReqData))
    }
}
