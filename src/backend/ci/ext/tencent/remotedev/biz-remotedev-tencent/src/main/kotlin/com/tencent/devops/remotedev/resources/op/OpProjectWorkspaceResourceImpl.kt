package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.api.op.OpProjectWorkspaceResource
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpProjectWorkspaceResourceImpl @Autowired constructor(
    private val workspaceCommon: WorkspaceCommon,
    private val createControl: CreateControl,
    private val workspaceService: WorkspaceService,
    private val windowsResourceConfigService: WindowsResourceConfigService
) : OpProjectWorkspaceResource {
    override fun assignWorkspace(
        userId: String,
        projectId: String,
        owner: String,
        cgsIds: List<String>
    ): Result<Boolean> {
        cgsIds.forEach { cgsId ->
            // 先校验该cgsId是否已被申领分配并运行中
            if (!workspaceCommon.checkCgsRunning(cgsId, EnvStatusEnum.running)) return Result(false)
            // 根据cgsId获取对应的机型和地域
            val cgsData = workspaceCommon.getCgsData(cgsId) ?: return Result(false)
            // 再根据机型和地域获取硬件资源配置
            val windowsResourceConfigId = windowsResourceConfigService.getConfig(
                zoneId = cgsData.zoneId,
                machineType = cgsData.machineType
            ) ?: return Result(false)
            // 调用CreateControl.asyncCreateWorkspace发起创建
            createControl.asyncCreateWorkspace(
                pmUserId = owner,
                projectId = projectId,
                cgsId = cgsId,
                autoAssign = true,
                workspaceCreate = ProjectWorkspaceCreate(
                    windowsResourceConfigId = windowsResourceConfigId.id!!.toInt(),
                    baseImageId = 0,
                    count = 1
                )
            )
        }
        return Result(true)
    }

    override fun getProjectWorkspaceList(
        userId: String,
        projectId: String?,
        workspaceName: String?,
        systemType: WorkspaceSystemType?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4Op(projectId, workspaceName, systemType, page, pageSize))
    }
}
