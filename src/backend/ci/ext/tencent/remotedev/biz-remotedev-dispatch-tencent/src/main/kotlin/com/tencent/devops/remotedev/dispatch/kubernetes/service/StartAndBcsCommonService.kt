package com.tencent.devops.remotedev.dispatch.kubernetes.service

import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceBcsClient
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.interfaces.ServiceRemoteDevInterface
import com.tencent.devops.remotedev.pojo.expert.CreateDiskData
import com.tencent.devops.remotedev.pojo.expert.CreateDiskDataClass
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatusEnum
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskData
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * start和bcs 有很多底层已经全是 bcs 了，对于公共方法，抽在这里在工厂方法引用
 */
@Service
class StartAndBcsCommonService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val bcsClient: WorkspaceBcsClient,
    private val workspaceRedisUtils: WorkspaceRedisUtils
) {
    @Suppress("ComplexMethod")
    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        logger.info("StartAndBcsCommonService|workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        val task = workspaceOpHisDao.getTask(dslContext, taskStatus.uid) ?: kotlin.run {
            logger.warn("workspaceTaskCallback|fail with wrong task|$taskStatus")
            return false
        }
        workspaceRedisUtils.refreshTaskStatus("bcs", taskStatus.uid, taskStatus)
        when {
            task.action == EnvironmentAction.EXPAND_DISK -> {
                kotlin.runCatching {
                    SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceExpandDiskCallback(
                        taskId = taskStatus.uid,
                        workspaceName = task.workspaceName,
                        operator = task.operator
                    )
                }.onFailure {
                    logger.warn("workspaceTaskCallback|workspaceExpandDiskCallback fail ${it.message}", it)
                }
                workspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = task.uid,
                    status = if (taskStatus.status == TaskStatusEnum.successed) {
                        EnvironmentActionStatus.SUCCEEDED
                    } else {
                        EnvironmentActionStatus.FAILED
                    },
                    actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
                )
                return true
            }

            task.action == EnvironmentAction.CREATE_DISK -> {
                kotlin.runCatching {
                    SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).workspaceCreateDiskCallback(
                        taskId = taskStatus.uid,
                        workspaceName = task.workspaceName,
                        operator = task.operator
                    )
                }.onFailure {
                    logger.warn("workspaceTaskCallback|workspaceCreateDiskCallback fail ${it.message}", it)
                }
                workspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = task.uid,
                    status = if (taskStatus.status == TaskStatusEnum.successed) {
                        EnvironmentActionStatus.SUCCEEDED
                    } else {
                        EnvironmentActionStatus.FAILED
                    },
                    actionMsg = taskStatus.logs.ifEmpty { null }?.joinToString(";")
                )
                return true
            }

            task.action == EnvironmentAction.UPGRADE_VM && task.status == EnvironmentActionStatus.PENDING -> {
                val result = kotlin.runCatching {
                    SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java)
                        .createWinWorkspaceByVm(
                            userId = task.operator,
                            oldWorkspaceName = task.workspaceName,
                            projectId = null,
                            ownerType = null,
                            uid = taskStatus.uid
                        ).data!!
                }.onFailure {
                    logger.warn("workspaceTaskCallback|upgradeVm error ${it.message}", it)
                }.getOrElse { false }
                if (result) {
                    workspaceOpHisDao.update(
                        dslContext, task.uid, EnvironmentActionStatus.SUCCEEDED
                    )
                } else {
                    workspaceOpHisDao.update(
                        dslContext, task.uid, EnvironmentActionStatus.FAILED
                    )
                    logger.error("workspaceTaskCallback $task error $taskStatus")
                }
                return true
            }

            task.action == EnvironmentAction.CREATE && task.status.needFix() -> {
                val oldWs = dispatchWorkspaceDao.getWorkspaceInfo(task.workspaceName, dslContext) ?: kotlin.run {
                    logger.warn("workspaceTaskCallback|try to fix fail with wrong workspace|$task")
                    return false
                }
                kotlin.runCatching {
                    SpringContextUtil.getBean(ServiceRemoteDevInterface::class.java).createWinWorkspaceByVm(
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

            task.action == EnvironmentAction.MAKE_IMAGE -> {
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
                workspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = task.uid,
                    status = if (taskStatus.status == TaskStatusEnum.successed) {
                        EnvironmentActionStatus.SUCCEEDED
                    } else {
                        EnvironmentActionStatus.FAILED
                    },
                    actionMsg = null
                )
                if (taskStatus.status == TaskStatusEnum.successed) {
                    dispatchWorkspaceDao.updateWorkspaceStatus(
                        workspaceName = task.workspaceName,
                        status = EnvStatusEnum.stopped,
                        dslContext = dslContext
                    )
                }
                return true
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

    fun createDisk(
        userId: String,
        workspaceName: String,
        size: String
    ): CreateDiskResp {
        // TODO: 在创建前应该还有一步检查是否可以创建，等接口
        val envId = getEnvironmentUid(workspaceName)
        val data = CreateDiskData(uid = envId, pvcSize = size, pvcClass = CreateDiskDataClass.HDD.data)
        val taskId = bcsClient.createDisk(data)?.taskUid ?: run {
            logger.warn("createDisk $workspaceName|$size taskId is null")
            return CreateDiskResp(false, "taskId is null")
        }
        workspaceOpHisDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            environmentUid = envId,
            operator = userId,
            action = EnvironmentAction.CREATE_DISK,
            uid = taskId
        )
        return CreateDiskResp(true, null)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartAndBcsCommonService::class.java)
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No bcs environment with $workspaceName")
    }
}
