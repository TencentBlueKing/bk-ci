package com.tencent.bk.codecc.task.listener

import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.PipelineIdRelationshipEntity
import com.tencent.bk.codecc.task.service.PipelineIdRelationService
import com.tencent.bk.codecc.task.service.TaskService
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GongfengRetryRecordListener @Autowired constructor(
    private val pipelineIdRelationService: PipelineIdRelationService,
    private val taskService: TaskService,
    private val taskRepository: TaskRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GongfengRetryRecordListener::class.java)
    }

    /**
     * 该方法功能：1.重试当天失败的项目；2.删除5日之前的记录，保证表数据数量不至于递增
     */
    fun retryAllFailRecord() {
        val pipelineNotSuccessList = pipelineIdRelationService.findAllFailOrProcessRecord()
        logger.info("not success list size: ${pipelineNotSuccessList.size}")
        pipelineNotSuccessList.forEach {
            try {
                val taskInfoEntity = taskRepository.findByPipelineId(it.pipelineId)
                if (null != taskInfoEntity && taskInfoEntity.taskId > 0L) {
                    logger.info("trigger task info entity, task id: ${taskInfoEntity.taskId}")
                    taskService.manualExecuteTaskNoProxy(taskInfoEntity.taskId, "false", "CodeCC")
                    val pipelineIdRelationshipEntity = PipelineIdRelationshipEntity(
                        it.taskId,
                        it.projectId,
                        it.pipelineId,
                        ComConstants.ScanStatus.PROCESSING.code,
                        LocalDate.now()
                    )
                    pipelineIdRelationService.updateFailOrProcessRecord(pipelineIdRelationshipEntity)
                    Thread.sleep(200L)
                }
            } catch (e: Exception) {
                logger.info("trigger pipeline fail! pipeline id: ${it.pipelineId}, error message: ${e.message}")
            }
        }
        logger.info("start to delete expired records")
        pipelineIdRelationService.deleteExpiredRecord()
    }
}