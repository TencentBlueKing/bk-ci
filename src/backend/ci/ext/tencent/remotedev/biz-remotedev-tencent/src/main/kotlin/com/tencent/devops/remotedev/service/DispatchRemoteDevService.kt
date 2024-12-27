package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.interfaces.ServiceRemoteDevInterface
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import com.tencent.devops.remotedev.service.projectworkspace.MakeWorkspaceImageHandler
import com.tencent.devops.remotedev.service.projectworkspace.RebuildWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.RestartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StopWorkspaceHandler
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class DispatchRemoteDevService(
    private val createControl: CreateControl,
    private val expertSupportService: ExpertSupportService,
    private val deleteControl: DeleteControl,
    private val startWorkspaceHandler: StartWorkspaceHandler,
    private val stopWorkspaceHandler: StopWorkspaceHandler,
    private val restartWorkspaceHandler: RestartWorkspaceHandler,
    private val rebuildWorkspaceHandler: RebuildWorkspaceHandler,
    private val makeWorkspaceImageHandler: MakeWorkspaceImageHandler
) : ServiceRemoteDevInterface {
    companion object {
        private val logger = LoggerFactory.getLogger(DispatchRemoteDevService::class.java)
    }

    override fun workspaceExpandDiskCallback(taskId: String, workspaceName: String, operator: String) {
        expertSupportService.expandDiskCallback(taskId, workspaceName, operator)
    }

    override fun createWinWorkspaceByVm(
        userId: String,
        oldWorkspaceName: String?,
        projectId: String?,
        ownerType: WorkspaceOwnerType?,
        uid: String,
        bak: Boolean
    ): Result<Boolean> {
        val res = createControl.createWinWorkspaceByVm(
            userId = userId,
            oldWorkspaceName = oldWorkspaceName,
            projectCode = projectId,
            ownerType = ownerType,
            uid = uid,
            bak = bak
        )
        return Result(res)
    }

    override fun makeImageCallback(
        taskId: String,
        workspaceName: String,
        operator: String,
        imageId: String
    ) {
        makeWorkspaceImageHandler.makeWorkspaceImageCallback(
            taskId = taskId,
            userId = operator,
            workspaceName = workspaceName,
            imageId = imageId
        )
    }

    override fun workspaceUpdate(event: RemoteDevUpdateEvent) {
        logger.info("A message is received from dispatch k8s $event")
        kotlin.runCatching {
            when (event.type) {
                UpdateEventType.CREATE -> createControl.afterCreateWorkspace(event)
                UpdateEventType.START -> startWorkspaceHandler.startWorkspaceCallback(event)
                UpdateEventType.STOP -> stopWorkspaceHandler.stopWorkspaceCallback(event)
                UpdateEventType.RESTART -> restartWorkspaceHandler.restartWorkspaceCallback(event)
                UpdateEventType.DELETE -> deleteControl.afterDeleteWorkspace(event)
                UpdateEventType.REBUILD -> rebuildWorkspaceHandler.rebuildWorkspaceCallback(event)
                else -> {}
            }
        }.onFailure {
            logger.warn("RemoteDevUpdateEvent call back error", it)
        }
    }

    override fun workspaceCreateDiskCallback(taskId: String, workspaceName: String, operator: String) {
        return expertSupportService.createDiskCallback(taskId, workspaceName, operator)
    }
}
