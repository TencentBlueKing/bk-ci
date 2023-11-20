package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.api.op.OpProjectWorkspaceResource
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminData
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
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
    private val gitProxyService: GitProxyService
) : OpProjectWorkspaceResource {
    override fun assignWorkspace(
        userId: String,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        val cgsData = workspaceCommon.getCgsData(data.cgsIds, data.ips) ?: return Result(false)
        cgsData.forEach { cgs ->
            // 先校验该cgsId是否已被申领分配并运行中
            if (!workspaceCommon.checkCgsRunning(cgs.cgsId, EnvStatusEnum.running)) return Result(false)
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
            Thread.sleep(1000)
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
        return workspaceService.exportProjectWorkspaceList(data)
    }
}
