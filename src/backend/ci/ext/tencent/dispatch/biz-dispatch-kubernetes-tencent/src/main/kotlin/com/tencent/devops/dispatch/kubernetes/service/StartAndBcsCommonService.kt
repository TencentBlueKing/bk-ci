package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.kubernetes.bcs.client.WorkspaceBcsClient
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ExpandDiskData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ExpandDiskValidateResp
import com.tencent.devops.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * start和bcs 有很多底层已经全是 bcs 了，对于公共方法，抽在这里在工厂方法引用
 */
@Service
class StartAndBcsCommonService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val bcsClient: WorkspaceBcsClient,
    private val workspaceRedisUtils: WorkspaceRedisUtils
) {
    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        logger.info("workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        val task = workspaceOpHisDao.getTask(dslContext, taskStatus.uid)
        workspaceRedisUtils.refreshTaskStatus("bcs", taskStatus.uid, taskStatus)
        if (task?.action == EnvironmentAction.EXPAND_DISK) {
            kotlin.runCatching {
                client.get(ServiceRemoteDevResource::class).workspaceExpandDiskCallback(
                    taskId = taskStatus.uid,
                    workspaceName = task.workspaceName,
                    operator = task.operator
                )
            }.onFailure {
                logger.warn("workspaceTaskCallback|workspaceExpandDiskCallback fail ${it.message}", it)
            }
            if (task.status == EnvironmentActionStatus.SUCCEEDED) {
                workspaceOpHisDao.updateStatusByWorkspaceName(
                    dslContext = dslContext,
                    workspaceName = task.workspaceName,
                    status = EnvironmentActionStatus.SUCCEEDED,
                    fStatus = EnvironmentActionStatus.PENDING
                )
            } else {
                workspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = taskStatus.uid,
                    status = task.status,
                    actionMsg = task.actionMsg
                )
            }
            return true
        }
        if (task?.status?.needFix() == true && task.action == EnvironmentAction.CREATE) {
            val oldWs = dispatchWorkspaceDao.getWorkspaceInfo(task.workspaceName, dslContext) ?: kotlin.run {
                logger.warn("workspaceTaskCallback|try to fix fail with wrong workspace|$task")
                return false
            }
            kotlin.runCatching {
                client.get(ServiceRemoteDevResource::class).createWinWorkspaceByVm(
                    userId = oldWs.userId,
                    oldWorkspaceName = oldWs.workspaceName,
                    projectId = null,
                    ownerType = null,
                    uid = taskStatus.uid
                )
            }.onFailure {
                logger.warn("workspaceTaskCallback|createWinWorkspaceByVm fail ${it.message}", it)
            }
        }
        return true
    }

    fun expandDisk(
        userId: String,
        workspaceName: String,
        size: String
    ): ExpandDiskValidateResp {
        val envId = getEnvironmentUid(workspaceName)
        val expandData = ExpandDiskData(envId, size)
        val validateRes = bcsClient.expandDiskValidate(expandData) ?: run {
            logger.warn("expandDiskValidate $workspaceName|$size validateRes is null")
            return ExpandDiskValidateResp(false, "validateRes is null")
        }
        if (!validateRes.valid) {
            return validateRes
        }
        val taskId = bcsClient.expandDisk(expandData)?.taskUid ?: run {
            logger.warn("expandDisk $workspaceName|$size taskId is null")
            return ExpandDiskValidateResp(false, "taskId is null")
        }
        workspaceOpHisDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            environmentUid = envId,
            operator = userId,
            action = EnvironmentAction.EXPAND_DISK,
            uid = taskId
        )
        return validateRes
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartAndBcsCommonService::class.java)
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No bcs environment with $workspaceName")
    }
}