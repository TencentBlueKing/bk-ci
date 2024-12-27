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

package com.tencent.devops.remotedev.service.workspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.audit.ActionAuditContent.ASSIGNS_TEMPLATE
import com.tencent.devops.common.audit.ActionAuditContent.CGS_ASSIGN_USER_CONTENT
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_CODE_TEMPLATE
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class DeliverControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val sharedDao: WorkspaceSharedDao,
    private val permissionService: PermissionService,
    private val workspaceCommon: WorkspaceCommon,
    private val gitProxyTGitService: GitProxyTGitService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeliverControl::class.java)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_ASSIGN,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = CGS_ASSIGN_USER_CONTENT
    )
    fun assignUser2Workspace(
        userId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>,
        checkPermission: Boolean = true
    ) {
        logger.info("assignUser2Workspace|$userId|$workspaceName|$assigns")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        if (checkPermission) {
            permissionService.checkUserManager(userId, workspace.projectId)
        }
        val assign2Owner = assigns.firstOrNull { it.type == WorkspaceShared.AssignType.OWNER }
        val alreadyExist = sharedDao.fetchWorkspaceSharedInfo(dslContext, workspaceName)
        val existOwner = alreadyExist.firstOrNull { it.type == WorkspaceShared.AssignType.OWNER }
        logger.info("assignUser2Workspace|assign2Owner|$assign2Owner|alreadyExist|$alreadyExist")

        ActionAuditContext.current()
            .addAttribute(ASSIGNS_TEMPLATE, assigns.joinToString(",") { it.userId })
            .addAttribute(PROJECT_CODE_TEMPLATE, workspace.projectId)
            .setScopeId(workspace.projectId)

        when {
            existOwner == null && assign2Owner != null -> {
                logger.info("assignUser2Workspace|$userId|${assign2Owner.userId}")
                workspaceCommon.shareWorkspace(
                    workspaceName = workspaceName,
                    projectId = workspace.projectId,
                    operator = userId,
                    assigns = listOf(assign2Owner),
                    mountType = WorkspaceMountType.START,
                    ownerType = workspace.ownerType
                )
                workspaceCommon.updateHostMonitor(
                    workspaceName = workspaceName,
                    props = workspaceCommon.genWorkspaceCCInfo(
                        workspace.projectId,
                        workspace.displayName.ifBlank { workspaceName },
                        assign2Owner.userId
                    ),
                    type = workspace.workspaceSystemType
                )
                if (workspace.status.checkDistributing()) {
                    workspaceDao.updateWorkspaceStatus(
                        dslContext = dslContext,
                        workspaceName = workspace.workspaceName,
                        status = WorkspaceStatus.RUNNING
                    )
                }
            }

            existOwner != null && assign2Owner?.userId != existOwner.sharedUser -> {
                workspaceCommon.unShareWorkspace(
                    workspaceName = workspaceName,
                    operator = userId,
                    sharedUsers = listOf(existOwner.sharedUser),
                    mountType = WorkspaceMountType.START,
                    assignType = WorkspaceShared.AssignType.OWNER
                )
                if (assign2Owner != null) {
                    workspaceCommon.shareWorkspace(
                        workspaceName = workspaceName,
                        projectId = workspace.projectId,
                        operator = userId,
                        assigns = listOf(
                            ProjectWorkspaceAssign(
                                userId = assign2Owner.userId,
                                type = WorkspaceShared.AssignType.OWNER,
                                expiration = null
                            )
                        ),
                        mountType = WorkspaceMountType.START,
                        ownerType = workspace.ownerType
                    )
                    workspaceCommon.updateHostMonitor(
                        workspaceName = workspaceName,
                        props = workspaceCommon.genWorkspaceCCInfo(
                            workspace.projectId,
                            workspace.displayName.ifBlank { workspaceName },
                            assign2Owner.userId
                        ),
                        type = workspace.workspaceSystemType
                    )
                }
            }
        }
        val em = alreadyExist.asSequence()
            .filter { it.type == WorkspaceShared.AssignType.VIEWER }.map { m -> m.sharedUser }
        val add = assigns.filter { it.type == WorkspaceShared.AssignType.VIEWER && it.userId !in em }
        if (add.isNotEmpty()) {
            workspaceCommon.shareWorkspace(
                workspaceName = workspaceName,
                projectId = workspace.projectId,
                operator = userId,
                assigns = add,
                mountType = WorkspaceMountType.START,
                ownerType = workspace.ownerType
            )
        }

        val am = assigns.filter { it.type == WorkspaceShared.AssignType.VIEWER }.map { m -> m.userId }
        val reduce = alreadyExist.filter { it.type == WorkspaceShared.AssignType.VIEWER && it.sharedUser !in am }
        if (reduce.isNotEmpty()) {
            workspaceCommon.unShareWorkspace(
                workspaceName = workspaceName,
                operator = userId,
                sharedUsers = reduce.map { it.sharedUser },
                mountType = WorkspaceMountType.START
            )
        }

        // 同步tgit proxy
        gitProxyTGitService.refreshProjectTGitSpecUser(workspace.projectId, null)
    }
}
