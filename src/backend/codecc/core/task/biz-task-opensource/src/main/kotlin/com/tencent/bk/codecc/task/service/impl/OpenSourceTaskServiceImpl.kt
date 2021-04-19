package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongotemplate.BuildIdRelationDao
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.model.DisableTaskEntity
import com.tencent.bk.codecc.task.service.OpenSourceTaskService
import com.tencent.devops.common.web.mq.EXCHANGE_EXTERNAL_JOB
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.HashMap

@Service
class OpenSourceTaskServiceImpl @Autowired constructor(
    private val taskRepository: TaskRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val taskDao: TaskDao,
    private val buildIdRelationDao: BuildIdRelationDao
) : OpenSourceTaskService {

    companion object {
        private val logger = LoggerFactory.getLogger(OpenSourceTaskServiceImpl::class.java)
    }

    override fun stopTask(taskId: Long, disableReason: String, userName: String) {
        val taskInfo = taskRepository.findByTaskId(taskId)
        if (null == taskInfo) {
            logger.info("task info not exists!")
            return
        }
        val lastDisableTaskInfo = taskInfo.lastDisableTaskInfo ?: DisableTaskEntity()
        val executeTime = taskInfo.executeTime
        val executeDate = taskInfo.executeDate
        lastDisableTaskInfo.lastExecuteDate = executeDate
        lastDisableTaskInfo.lastExecuteTime = executeTime
        taskInfo.lastDisableTaskInfo = lastDisableTaskInfo
        taskInfo.executeDate = emptyList()
        taskInfo.executeTime = ""
        taskInfo.disableTime = System.currentTimeMillis().toString()
        taskInfo.disableReason = disableReason
        taskInfo.status = TaskConstants.TaskStatus.DISABLE.value()

        // 停止日报
        if (null != taskInfo.notifyCustomInfo && !taskInfo.notifyCustomInfo.reportJobName.isNullOrBlank()) {
            val jobExternalDto = JobExternalDto(
                taskInfo.notifyCustomInfo.reportJobName,
                "",
                "",
                "",
                HashMap(),
                OperationType.PARSE
            )
            rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto)
        }

        taskDao.updateEntity(taskInfo, userName)
        logger.info("disable task finish! task id: $taskId")
    }

    /**
     * 启用任务
     */
    override fun startTask(taskId: Long, userName: String) {
        logger.info("start task, task id: $taskId, user name : $userName")
        val taskInfo = taskRepository.findByTaskId(taskId)
        if (null == taskInfo) {
            logger.info("task info not exists")
            return
        }
        val lastDisableTaskInfo = taskInfo.lastDisableTaskInfo
        if (null != lastDisableTaskInfo) {
            val lastExecuteTime = lastDisableTaskInfo.lastExecuteTime
            val lastExecuteDate = lastDisableTaskInfo.lastExecuteDate
            taskInfo.executeTime = lastExecuteTime
            taskInfo.executeDate = lastExecuteDate
        }
        taskInfo.lastDisableTaskInfo = null
        taskInfo.disableTime = ""
        taskInfo.disableReason = ""
        taskInfo.status = TaskConstants.TaskStatus.ENABLE.value()
        val updateResult = taskDao.updateEntity(taskInfo, userName)
        logger.info("update result: $updateResult, task id: $taskId")
    }

    /**
     * 更新映射表的commit_id
     */
    override fun updateBuildCommitId(buildId: String, commitId: String) {
        logger.info("start to update commit id, build id: $buildId, commit id: $commitId")
        buildIdRelationDao.updateCommitId(buildId, commitId)
    }
}