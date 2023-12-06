package com.tencent.devops.remotedev.resources.op

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.api.op.OpProjectWorkspaceResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminData
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceXlsxExportService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class OpProjectWorkspaceResourceImpl @Autowired constructor(
    private val workspaceCommon: WorkspaceCommon,
    private val createControl: CreateControl,
    private val workspaceService: WorkspaceService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val gitProxyService: GitProxyService,
    private val xlsxExportService: WorkspaceXlsxExportService
) : OpProjectWorkspaceResource {
    @AuditEntry(
        actionId = ActionId.CGS_ASSIGN,
        subActionIds = [ActionId.CGS_CREATE]
    )
    @ActionAuditRecord(
        actionId = ActionId.CGS_ASSIGN,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS
        ),
        content = ActionAuditContent.CGS_ASSIGN_PROJECT_CONTENT
    )
    override fun assignWorkspace(
        userId: String,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        val cgsData = workspaceCommon.getCgsData(data.cgsIds, data.ips) ?: return Result(false)
        cgsData.forEach { cgs ->
            if (cgs.status != Constansts.CGS_AVAIABLE_STATUS) return@forEach
            // 先校验该cgsId是否已被申领分配并运行中
            if (!workspaceCommon.checkCgsRunning(cgs.cgsId, EnvStatusEnum.running)) return Result(false)
            // 审计
            ActionAuditContext.current()
                .addInstanceInfo(
                    cgs.cgsId,
                    cgs.cgsId,
                    null,
                    null
                )
                .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, data.projectId)
                .scopeId = data.projectId
            // 再根据机型和地域获取硬件资源配置
            val windowsResourceConfigId = windowsResourceConfigService.getTypeConfig(
                machineType = cgs.machineType
            ) ?: return Result(false)
            // 调用CreateControl.asyncCreateWorkspace发起创建
            createControl.asyncCreateWorkspace(
                pmUserId = userId,
                projectId = data.projectId,
                cgsId = cgs.cgsId,
                autoAssign = false,
                workspaceCreate = ProjectWorkspaceCreate(
                    windowsType = windowsResourceConfigId.size,
                    windowsZone = cgs.zoneId.replace(Regex("\\d+"), ""),
                    baseImageId = 0,
                    count = 1
                )
            )
            Thread.sleep(500)
        }
        return Result(true)
    }

    override fun getProjectWorkspaceList(
        userId: String,
        data: ProjectWorkspaceFetchData
    ): Result<Page<ProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4Op(data))
    }

    override fun fetchOwnerAndAdmin(
        userId: String,
        data: FetchOwnerAndAdminData
    ): Result<Set<String>> {
        return Result(desktopWorkspaceService.fetchOwnerAndAdmin(data))
    }

    override fun updateCCHost(userId: String, data: OpUpdateCCHostData): Result<Boolean> {
        return Result(desktopWorkspaceService.updateCCHost(data))
    }

    override fun refreshCodeProxy(userId: String, projectId: String) {
        gitProxyService.refreshCodeProxy(projectId)
    }

    override fun exportProjectWorkspaceList(userId: String, data: ProjectWorkspaceFetchData): Response {
        return xlsxExportService.exportProjectWorkspaceListOp(data)
    }
}
