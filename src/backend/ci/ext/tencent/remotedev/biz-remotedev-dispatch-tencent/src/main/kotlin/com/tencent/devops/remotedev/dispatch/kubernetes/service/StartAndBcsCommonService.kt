package com.tencent.devops.remotedev.dispatch.kubernetes.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.DispatchWorkspaceOpHisRecord
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.CLONE_VM
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.CREATE
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.DELETE
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.DELETE_VM
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.EXPAND_DISK
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.MAKE_IMAGE
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.REBUILD
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.RECREATE
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.RESTART
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.SCALE
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.START
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.STOP
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction.UPGRADE_VM
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceBcsClient
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentOperate
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.interfaces.ServiceRemoteDevInterface
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * start和bcs 有很多底层已经全是 bcs 了，对于公共方法，抽在这里在工厂方法引用
 */
@Service
class StartAndBcsCommonService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceBcsClient: WorkspaceBcsClient,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val workspaceRedisUtils: WorkspaceRedisUtils
) {
    @Suppress("ComplexMethod")
    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        logger.info("StartAndBcsCommonService|workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        val task = dispatchWorkspaceOpHisDao.getTask(dslContext, taskStatus.uid) ?: kotlin.run {
            logger.warn("workspaceTaskCallback|fail with wrong task|$taskStatus")
            return false
        }
        workspaceRedisUtils.refreshTaskStatus("bcs", taskStatus.uid, taskStatus)
        logger.info("StartAndBcsCommonService|workspaceTaskCallback|task info|$task")
        when {
            task.status == EnvironmentActionStatus.PENDING -> dealWithPending(taskStatus, task)
            task.status.needFix() -> dealWithFix(taskStatus, task)
        }
        return true
    }

    private fun dealWithFix(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        when {
            task.action == CREATE -> {
                fixVm(task, taskStatus)
            }
        }
    }

    private fun dealWithPending(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        when (task.action) {
            CREATE -> createWorkspace(taskStatus, task.workspaceName, task.operator)
            START -> startWorkspace(taskStatus, task)
            STOP -> stopWorkspace(taskStatus, task)
            RECREATE -> Unit /*考虑下掉*/
            SCALE -> Unit /*考虑下掉*/
            DELETE -> Unit /*考虑下掉*/
            DELETE_VM -> deleteWorkspace(taskStatus, task)
            RESTART -> restartWorkspace(taskStatus, task)
            MAKE_IMAGE -> makeImage(taskStatus, task)
            REBUILD -> rebuildWorkspace(taskStatus, task)
            EXPAND_DISK -> expandDisk(taskStatus, task)
            UPGRADE_VM -> bakOldCreateNewVm(task, taskStatus)
            CLONE_VM -> cloneCreateNewVm(task, taskStatus)
        }
    }

    fun createWorkspace(
        taskStatus: TaskStatus,
        workspaceName: String,
        operator: String
    ) {
        if (taskStatus.status == TaskStatusEnum.successed) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskStatus.uid, EnvironmentActionStatus.SUCCEEDED
            )
            dispatchWorkspaceDao.updateWorkspace(
                workspaceName = workspaceName,
                envId = taskStatus.vmCreateResp?.envId ?: "",
                regionId = taskStatus.vmCreateResp?.cloudZoneId?.toInt() ?: 0,
                dslContext = dslContext
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = operator,
                    workspaceName = workspaceName,
                    type = UpdateEventType.CREATE,
                    status = true,
                    environmentUid = taskStatus.vmCreateResp?.envId,
                    environmentHost = taskStatus.vmCreateResp?.cgsIp,
                    environmentIp = taskStatus.vmCreateResp?.cgsIp?.substringAfter("."),
                    resourceId = taskStatus.vmCreateResp?.resourceId,
                    macAddress = taskStatus.vmCreateResp?.macAddress
                )
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext = dslContext,
                uid = taskStatus.uid,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = operator,
                    workspaceName = workspaceName,
                    type = UpdateEventType.CREATE,
                    status = false,
                    environmentUid = taskStatus.vmCreateResp?.envId,
                    environmentHost = taskStatus.vmCreateResp?.cgsIp,
                    environmentIp = taskStatus.vmCreateResp?.cgsIp?.substringAfter("."),
                    resourceId = taskStatus.vmCreateResp?.resourceId,
                    macAddress = taskStatus.vmCreateResp?.macAddress,
                    errorMsg = JsonUtil.toJson(taskStatus)
                )
            )
        }
    }

    private fun startWorkspace(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        if (taskStatus.status == TaskStatusEnum.successed) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
            val workspaceStatus = workspaceBcsClient.startGetWorkspaceInfo(
                userId = task.operator,
                environmentOperate = EnvironmentOperate(task.envId)
            )
            if (workspaceStatus.status == EnvStatusEnum.running) {
                SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                    event = RemoteDevUpdateEvent(
                        userId = task.operator,
                        workspaceName = task.workspaceName,
                        type = UpdateEventType.START,
                        status = true,
                        environmentIp = workspaceStatus.environmentIP
                    )
                )
                return
            }
        }

        dispatchWorkspaceOpHisDao.update(
            dslContext = dslContext,
            uid = taskStatus.uid,
            status = EnvironmentActionStatus.FAILED,
            fStatus = EnvironmentActionStatus.PENDING,
            actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
        )
        SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
            event = RemoteDevUpdateEvent(
                userId = task.operator,
                workspaceName = task.workspaceName,
                type = UpdateEventType.START,
                status = false,
                errorMsg = JsonUtil.toJson(taskStatus)
            )
        )
    }

    private fun stopWorkspace(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        if (taskStatus.status == TaskStatusEnum.successed) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.STOP,
                    status = true
                )
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext = dslContext,
                uid = taskStatus.uid,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.STOP,
                    status = false,
                    errorMsg = JsonUtil.toJson(taskStatus)
                )
            )
        }
    }

    private fun deleteWorkspace(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        if (taskStatus.status == TaskStatusEnum.successed) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.DELETE,
                    status = true
                )
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext = dslContext,
                uid = taskStatus.uid,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.DELETE,
                    status = false,
                    errorMsg = JsonUtil.toJson(taskStatus)
                )
            )
        }
    }

    private fun restartWorkspace(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        if (taskStatus.status == TaskStatusEnum.successed) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.RESTART,
                    status = true
                )
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext = dslContext,
                uid = taskStatus.uid,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.RESTART,
                    status = false,
                    errorMsg = JsonUtil.toJson(taskStatus)
                )
            )
        }
    }

    private fun rebuildWorkspace(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        if (taskStatus.status == TaskStatusEnum.successed) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.REBUILD,
                    status = true
                )
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext = dslContext,
                uid = taskStatus.uid,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
            )
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceUpdate(
                event = RemoteDevUpdateEvent(
                    userId = task.operator,
                    workspaceName = task.workspaceName,
                    type = UpdateEventType.REBUILD,
                    status = false,
                    errorMsg = JsonUtil.toJson(taskStatus)
                )
            )
        }
    }

    private fun makeImage(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        kotlin.runCatching {
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).makeImageCallback(
                taskId = taskStatus.uid,
                workspaceName = task.workspaceName,
                operator = task.operator,
                imageId = task.actionMsg
            )
        }.onFailure {
            logger.warn("workspaceTaskCallback|makeImageCallback fail ${it.message}", it)
        }
        dispatchWorkspaceOpHisDao.update(
            dslContext = dslContext,
            uid = task.uid,
            status = if (taskStatus.status == TaskStatusEnum.successed) {
                EnvironmentActionStatus.SUCCEEDED
            } else {
                EnvironmentActionStatus.FAILED
            },
            actionMsg = null
        )
    }

    private fun fixVm(
        task: DispatchWorkspaceOpHisRecord,
        taskStatus: TaskStatus
    ) {
        val oldWs = dispatchWorkspaceDao.getWorkspaceInfo(task.workspaceName, dslContext) ?: kotlin.run {
            logger.warn("workspaceTaskCallback|try to fix fail with wrong workspace|$task")
            return
        }
        kotlin.runCatching {
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).createWinWorkspaceByVm(
                userId = oldWs.userId,
                oldWorkspaceName = oldWs.workspaceName,
                projectId = null,
                ownerType = null,
                uid = taskStatus.uid,
                bak = true
            )
        }.onFailure {
            logger.warn("workspaceTaskCallback|createWinWorkspaceByVm fail ${it.message}", it)
        }
    }

    private fun bakOldCreateNewVm(
        task: DispatchWorkspaceOpHisRecord,
        taskStatus: TaskStatus
    ) {
        val result = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java)
                .createWinWorkspaceByVm(
                    userId = task.operator,
                    oldWorkspaceName = task.workspaceName,
                    projectId = null,
                    ownerType = null,
                    uid = taskStatus.uid,
                    bak = true
                ).data!!
        }.onFailure {
            logger.warn("workspaceTaskCallback|createNewVm error ${it.message}", it)
        }.getOrElse { false }
        if (result) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.FAILED
            )
            logger.error("workspaceTaskCallback $task error $taskStatus")
        }
    }

    private fun cloneCreateNewVm(
        task: DispatchWorkspaceOpHisRecord,
        taskStatus: TaskStatus
    ) {
        val result = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java)
                .createWinWorkspaceByVm(
                    userId = task.operator,
                    oldWorkspaceName = task.workspaceName,
                    projectId = null,
                    ownerType = null,
                    uid = taskStatus.uid,
                    bak = false
                ).data!!
        }.onFailure {
            logger.warn("workspaceTaskCallback|cloneVm error ${it.message}", it)
        }.getOrElse { false }
        if (result) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext = dslContext,
                uid = taskStatus.uid,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
            )
            logger.error("workspaceTaskCallback $task error $taskStatus")
        }
    }

    private fun expandDisk(
        taskStatus: TaskStatus,
        task: DispatchWorkspaceOpHisRecord
    ) {
        kotlin.runCatching {
            SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceExpandDiskCallback(
                taskId = taskStatus.uid,
                workspaceName = task.workspaceName,
                operator = task.operator
            )
        }.onFailure {
            logger.warn("workspaceTaskCallback|workspaceExpandDiskCallback fail ${it.message}", it)
        }
        dispatchWorkspaceOpHisDao.update(
            dslContext = dslContext,
            uid = task.uid,
            status = if (taskStatus.status == TaskStatusEnum.successed) {
                EnvironmentActionStatus.SUCCEEDED
            } else {
                EnvironmentActionStatus.FAILED
            },
            actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartAndBcsCommonService::class.java)
    }
}
