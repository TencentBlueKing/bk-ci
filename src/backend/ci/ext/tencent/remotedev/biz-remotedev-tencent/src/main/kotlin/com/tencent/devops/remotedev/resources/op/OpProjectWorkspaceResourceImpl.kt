package com.tencent.devops.remotedev.resources.op

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.auth.api.TencentResourceTypeId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpProjectWorkspaceResource
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyListData
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceRecordService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceXlsxExportService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class OpProjectWorkspaceResourceImpl @Autowired constructor(
    private val createControl: CreateControl,
    private val workspaceService: WorkspaceService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val xlsxExportService: WorkspaceXlsxExportService,
    private val workspaceRecordService: WorkspaceRecordService,
    private val notifyControl: NotifyControl,
    private val deliverControl: DeliverControl
) : OpProjectWorkspaceResource {
    @AuditEntry(
        actionId = TencentActionId.CGS_ASSIGN,
        subActionIds = [TencentActionId.CGS_CREATE]
    )
    @ActionAuditRecord(
        actionId = TencentActionId.CGS_ASSIGN,
        instance = AuditInstanceRecord(
            resourceType = TencentResourceTypeId.CGS
        ),
        content = TencentActionAuditContent.CGS_ASSIGN_PROJECT_CONTENT
    )
    override fun assignWorkspace(
        userId: String,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        logger.info("op assignWorkspace|$userId|$data")
        // 分配之前先同步下最新的数据
        createControl.assignWorkspace(userId, data)
        return Result(true)
    }

    override fun getProjectWorkspaceList(
        userId: String,
        data: ProjectWorkspaceFetchData
    ): Result<Page<ProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4Op(userId, data))
    }

    override fun updateCCHost(userId: String, data: OpUpdateCCHostData): Result<Boolean> {
        return Result(desktopWorkspaceService.updateCCHost(data))
    }

    override fun exportProjectWorkspaceList(userId: String, data: ProjectWorkspaceFetchData): Response {
        return xlsxExportService.exportProjectWorkspaceListOp(userId, data)
    }

    override fun notify(userId: String, notifyData: WorkspaceNotifyData): Result<Boolean> {
        notifyControl.notifyWorkspaceInfo(
            userId = userId,
            notifyData = notifyData
        )
        return Result(true)
    }

    override fun fetchNotifyList(userId: String, page: Int, pageSize: Int): Result<List<WorkspaceNotifyListData>> {
        return Result(notifyControl.fetchNotifyList(page, pageSize))
    }

    override fun applyViewRecordCallback(userId: String, projectId: String, workspaceName: String) {
        workspaceRecordService.approvalRecordViewCallback(
            projectId = projectId,
            userId = userId,
            workspaceName = workspaceName
        )
    }

    override fun assignUser(
        userId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean> {
        deliverControl.assignUser2Workspace(
            userId = userId,
            workspaceName = workspaceName,
            assigns = assigns,
            checkPermission = false
        )

        return Result(true)
    }

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

    companion object {
        private val logger = LoggerFactory.getLogger(OpProjectWorkspaceResourceImpl::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssignWorkspacePipelineInfo(
    val userId: String?,
    val projectId: String,
    val pipelineId: String,
    val buildParam: Map<String, String>
)
