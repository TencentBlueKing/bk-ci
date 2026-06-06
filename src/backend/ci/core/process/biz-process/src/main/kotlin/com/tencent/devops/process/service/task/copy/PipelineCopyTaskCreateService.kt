package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.service.task.PipelineBatchTaskFactory
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyTaskCreateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao,
    private val redisOperation: RedisOperation,
    private val pipelineBatchTaskFactory: PipelineBatchTaskFactory,
    private val pipelineCopyTaskFactory: PipelineCopyTaskFactory,
    private val pipelineCopyTaskStateService: PipelineCopyTaskStateService
) {

    fun create(event: PipelineBatchTaskCreateEvent) {
        with(event) {
            logger.info("start to handler create pipeline copy task|$projectId|$taskId")
            tryStartCreate(
                projectId = projectId,
                taskId = taskId
            ) ?: return
            try {
                doCreate(
                    projectId = projectId,
                    taskId = taskId,
                    taskType = taskType
                )
            } catch (ignored: Exception) {
                logger.error("Failed to create pipeline copy task|$projectId|$taskId", ignored)
                pipelineCopyTaskStateService.updateTaskStatusWithLock(
                    projectId = projectId,
                    taskId = taskId,
                    status = PipelineBatchTaskStatus.PIPELINE_ANALYZE_FAILED,
                    errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
                )
            }
        }
    }

    private fun doCreate(
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType
    ) {
        val details = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        val subPipelineResourceRels = pipelineCopyTaskFactory.buildSubPipelineResourceRels(
            projectId = projectId,
            details = details
        )
        val existsPipelineIds = details.map { it.pipelineId }.toSet()
        val subPipelineIds = subPipelineResourceRels
            .map { it.resourceId }
            .distinct()
            .filterNot { existsPipelineIds.contains(it) }
        val subDetails = pipelineBatchTaskFactory.buildBatchTaskDetail(
            projectId = projectId,
            taskId = taskId,
            taskType = taskType,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY,
            pipelineIds = subPipelineIds,
            subPipeline = true
        )
        val allDetails = mutableListOf<PipelineBatchTaskDetail>().apply {
            addAll(details)
            addAll(subDetails)
        }
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        // 存储时再加锁
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineBatchTaskDetailDao.batchCreate(
                    dslContext = transactionContext,
                    details = subDetails
                )
                pipelineCopyTaskResourceRelDao.batchCreate(
                    dslContext = transactionContext,
                    relations = subPipelineResourceRels
                )
                // 子流水线分析完成后,把任务状态更新为DRAFT
                pipelineBatchTaskDao.update(
                    dslContext = transactionContext,
                    update = PipelineBatchTaskUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        status = PipelineBatchTaskStatus.DRAFT,
                        subPipelineCount = allDetails.count { it.subPipeline },
                        pacCount = allDetails.count { it.pac },
                        clearErrorMessage = true
                    )
                )
            }
        } finally {
            lock.unlock()
        }
    }

    private fun tryStartCreate(
        projectId: String,
        taskId: String
    ): PipelineBatchTask? {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        // 分析阶段,耗时比较长,锁只需要加在更新状态阶段
        try {
            lock.lock()
            val task = pipelineBatchTaskDao.get(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId
            ) ?: run {
                logger.warn("pipeline batch task has no change, no need to analyze|$projectId|$taskId")
                return null
            }
            if (task.taskType != PipelineBatchTaskType.PIPELINE_COPY) {
                logger.warn("pipeline batch task type not match|$projectId|$taskId")
                return null
            }
            if (task.status !in setOf(
                    PipelineBatchTaskStatus.PIPELINE_ANALYZING,
                    PipelineBatchTaskStatus.PIPELINE_ANALYZE_FAILED
                )
            ) {
                logger.warn("pipeline batch task status not match|$projectId|$taskId|${task.status}")
                return null
            }
            if (task.status == PipelineBatchTaskStatus.PIPELINE_ANALYZE_FAILED) {
                pipelineBatchTaskDao.update(
                    dslContext = dslContext,
                    update = PipelineBatchTaskUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        status = PipelineBatchTaskStatus.PIPELINE_ANALYZING,
                        clearErrorMessage = true
                    )
                )
            }
            return task
        } finally {
            lock.unlock()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskCreateService::class.java)
    }
}
