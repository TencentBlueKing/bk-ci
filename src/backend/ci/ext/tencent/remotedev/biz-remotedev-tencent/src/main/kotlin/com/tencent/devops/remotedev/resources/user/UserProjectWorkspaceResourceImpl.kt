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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectWorkspaceResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceCloneReq
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusResp
import com.tencent.devops.remotedev.pojo.windows.TimeScope
import com.tencent.devops.remotedev.pojo.windows.UserLoginTimeResp
import com.tencent.devops.remotedev.service.BKBaseService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.StartWorkspaceService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceRecordService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceXlsxExportService
import com.tencent.devops.remotedev.service.projectworkspace.CloneWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.MakeWorkspaceImageHandler
import com.tencent.devops.remotedev.service.projectworkspace.RebuildWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.RestartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StopWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.UpgradeWorkspaceHandler
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import javax.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserProjectWorkspaceResourceImpl @Autowired constructor(
    private val workspaceService: WorkspaceService,
    private val permissionService: PermissionService,
    private val createControl: CreateControl,
    private val deliverControl: DeliverControl,
    private val deleteControl: DeleteControl,
    private val startWorkspaceHandler: StartWorkspaceHandler,
    private val stopWorkspaceHandler: StopWorkspaceHandler,
    private val restartWorkspaceHandler: RestartWorkspaceHandler,
    private val rebuildWorkspaceHandler: RebuildWorkspaceHandler,
    private val makeWorkspaceImageHandler: MakeWorkspaceImageHandler,
    private val upgradeWorkspaceHandler: UpgradeWorkspaceHandler,
    private val startWorkspaceService: StartWorkspaceService,
    private val bkBaseService: BKBaseService,
    private val xlsxExportService: WorkspaceXlsxExportService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val workspaceRecordService: WorkspaceRecordService,
    private val cloneWorkspaceHandler: CloneWorkspaceHandler
) : UserProjectWorkspaceResource {
    @AuditEntry(actionId = TencentActionId.CGS_CREATE)
    override fun createWorkspace(
        userId: String,
        projectId: String,
        workspace: WindowsWorkspaceCreate
    ): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        createControl.projectCreateWorkspace(
            pmUserId = userId,
            projectId = projectId,
            cgsId = null,
            workspaceCreate = workspace,
            zoneType = WindowsResourceZoneConfigType.DEFAULT
        )
        return Result(true)
    }

    @AuditEntry(actionId = TencentActionId.CGS_DELETE)
    override fun deleteWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        return Result(
            deleteControl.deleteWorkspace(
                userId = userId,
                workspaceName = workspaceName,
                needPermission = false
            )
        )
    }

    override fun getWorkspaceList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(workspaceService.getProjectWorkspaceList(userId, projectId, page, pageSize, null))
    }

    override fun getWorkspaceListNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<ProjectWorkspace>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(workspaceService.getProjectWorkspaceList(userId, projectId, page, pageSize, search))
    }

    @AuditEntry(actionId = TencentActionId.CGS_ASSIGN)
    override fun assignUser(
        userId: String,
        projectId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean> {
        deliverControl.assignUser2Workspace(userId, workspaceName, assigns)
        return Result(true)
    }

    override fun checkManager(userId: String, projectId: String): Result<Boolean> {
        return Result(permissionService.hasUserManager(userId, projectId))
    }

    @AuditEntry(actionId = TencentActionId.CGS_START)
    override fun startWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        startWorkspaceHandler.startWorkspace(userId, workspaceName)
        return Result(true)
    }

    @AuditEntry(actionId = TencentActionId.CGS_STOP)
    override fun stopWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        stopWorkspaceHandler.stopWorkspace(userId, workspaceName)
        return Result(true)
    }

    @AuditEntry(actionId = TencentActionId.CGS_RESTART)
    override fun restartWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        restartWorkspaceHandler.restartWorkspace(userId, workspaceName)
        return Result(true)
    }

    @AuditEntry(actionId = TencentActionId.CGS_MAKE_IMAGE)
    override fun makeImageByVm(
        userId: String,
        projectId: String,
        workspaceName: String,
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean> {
        makeWorkspaceImageHandler.makeWorkspaceImage(
            userId = userId,
            workspaceName = workspaceName,
            makeImageReq = makeImageReq
        )
        return Result(true)
    }

    override fun computerStatus(userId: String, projectId: String): Result<ComputerStatusResp> {
        if (!permissionService.checkUserVisitPermission(userId, projectId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
        return Result(startWorkspaceService.computerStatus(projectId))
    }

    override fun userLoginTime(userId: String, projectId: String, timeScope: TimeScope?): Result<UserLoginTimeResp> {
        if (!permissionService.checkUserVisitPermission(userId, projectId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
        return Result(
            bkBaseService.fetchOnlineUserMin(timeScope, projectId) ?: UserLoginTimeResp(0, emptyList())
        )
    }

    override fun exportWorkspaceList(userId: String, projectId: String, page: Int?, pageSize: Int?): Response {
        if (!permissionService.checkUserVisitPermission(userId, projectId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
        return xlsxExportService.exportProjectWorkspaceListWeb(userId, projectId, page, pageSize)
    }

    override fun reBuildWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        rebuildReq: WorkspaceRebuildReq
    ): Result<Boolean> {
        rebuildWorkspaceHandler.rebuildWorkspace(
            userId = userId,
            workspaceName = workspaceName,
            rebuildReq = rebuildReq
        )
        return Result(true)
    }

    override fun fetchSpec(
        userId: String,
        projectId: String,
        machineType: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WindowsSpecResInfo>> {
        if (!permissionService.checkUserVisitPermission(userId, projectId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
        return Result(windowsResourceConfigService.fetchSpec(projectId, machineType, page, pageSize))
    }

    override fun upgradeWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        upgradeReq: WorkspaceUpgradeReq
    ): Result<Boolean> {
        upgradeWorkspaceHandler.upgradeWorkspace(userId, projectId, workspaceName, upgradeReq)
        return Result(true)
    }

    override fun cloneWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        cloneReq: WorkspaceCloneReq
    ): Result<Boolean> {
        cloneWorkspaceHandler.cloneWorkspace(userId, projectId, workspaceName, cloneReq)
        return Result(true)
    }

    override fun applyViewRecord(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        permissionService.checkUserProjectManager(userId, projectId)
        workspaceRecordService.approvalRecordView(projectId = projectId, user = userId, workspaceName = workspaceName)
        return Result(true)
    }

    override fun getViewRecordMetadata(
        userId: String,
        projectId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?,
        startTime: Long,
        stopTime: Long
    ): Result<Page<WorkspaceRecordMetadata>> {
        permissionService.checkUserProjectManager(userId, projectId)
        if (!workspaceRecordService.checkWorkspaceUserApproval(workspaceName, userId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_RECORD_VIEW_NO_PERMISSION_ERROR.errorCode,
                params = arrayOf(userId, workspaceName)
            )
        }
        return Result(
            workspaceRecordService.getWorkspaceRecordMetadata(
                projectId = projectId,
                userId = userId,
                workspaceName = workspaceName,
                page = page,
                pageSize = pageSize,
                startTime = startTime,
                stopTime = stopTime
            )
        )
    }
}
