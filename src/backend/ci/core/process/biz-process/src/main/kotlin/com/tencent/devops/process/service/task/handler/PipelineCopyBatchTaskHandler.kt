package com.tencent.devops.process.service.task.handler

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskAnalyzeEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.service.PipelineDependentResourceService
import com.tencent.devops.process.service.task.PipelineBatchTaskFactory
import com.tencent.devops.process.service.task.PipelineBatchTaskHandler
import com.tencent.devops.process.service.task.PipelineCopyTaskAnalyzeService
import com.tencent.devops.process.service.task.PipelineCopyTaskExecuteService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyBatchTaskHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineDependentResourceService: PipelineDependentResourceService,
    private val pipelineBatchTaskFactory: PipelineBatchTaskFactory,
    private val pipelineCopyTaskAnalyzeService: PipelineCopyTaskAnalyzeService,
    private val pipelineCopyTaskExecuteService: PipelineCopyTaskExecuteService
) : PipelineBatchTaskHandler {

    override fun support(taskType: PipelineBatchTaskType): Boolean {
        return taskType == PipelineBatchTaskType.PIPELINE_COPY
    }

    override fun validateWhenCreate(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ) {
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, projectId)
            )
        }
    }

    override fun detailStatusWhenCreate(): PipelineBatchTaskDetailStatus {
        return PipelineBatchTaskDetailStatus.WAIT_COPY
    }

    override fun handleCreateEvent(event: PipelineBatchTaskCreateEvent) {
        getTask(projectId = event.projectId, taskId = event.taskId) ?: return
        val details = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = event.projectId,
            taskId = event.taskId
        )
        val subPipelineResources = details
            .filterNot { it.subPipeline }
            .flatMap { detail ->
                pipelineDependentResourceService.analysisSubPipelineDependency(
                    projectId = event.projectId,
                    pipelineId = detail.pipelineId
                )
            }
            .distinctBy { it.resourceId }
        val existsPipelineIds = details.map { it.pipelineId }.toSet()
        val subPipelineIds = subPipelineResources.filterNot {
            existsPipelineIds.contains(it.resourceId)
        }.map { it.resourceId }
        val subDetails = pipelineBatchTaskFactory.buildBatchTaskDetail(
            projectId = event.projectId,
            taskId = event.taskId,
            taskType = event.taskType,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY,
            pipelineIds = subPipelineIds,
            subPipeline = true
        )
        val allDetails = mutableListOf<PipelineBatchTaskDetail>().apply {
            addAll(details)
            addAll(subDetails)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBatchTaskDetailDao.batchCreate(
                dslContext = transactionContext,
                details = subDetails
            )
            pipelineBatchTaskDao.update(
                dslContext = transactionContext,
                update = PipelineBatchTaskUpdate(
                    projectId = event.projectId,
                    taskId = event.taskId,
                    subPipelineCount = allDetails.count { it.subPipeline },
                    pacCount = allDetails.count { it.pac }
                )
            )
        }
    }

    override fun handleAnalyzeEvent(event: PipelineBatchTaskAnalyzeEvent) {
        pipelineCopyTaskAnalyzeService.analyze(
            event = event
        )
    }

    override fun validateWhenExecute(userId: String, projectId: String, task: PipelineBatchTask) {
        val param = getParam(task)
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = param.targetProjectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, param.targetProjectId)
            )
        }
        val unfinished = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = task.taskId
        ).filter { it.status == PipelineCopyTaskResourceStatus.UNPROCESSED }
        if (unfinished.isNotEmpty()) {
            throw InvalidParamException("copy resources must be processed before execute")
        }
    }

    override fun handleExecuteEvent(event: PipelineBatchTaskExecuteEvent) {
        val task = getTask(projectId = event.projectId, taskId = event.taskId) ?: return
        pipelineCopyTaskExecuteService.execute(projectId = event.projectId, task = task)
    }

    private fun getTask(projectId: String, taskId: String): PipelineBatchTask? {
        val task = pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: run {
            logger.warn("pipeline batch task not found|$projectId|$taskId")
            return null
        }
        if (task.taskType != PipelineBatchTaskType.PIPELINE_COPY) {
            logger.warn("pipeline batch task type not match|$projectId|$taskId")
            return null
        }
        return task
    }

    private fun parseParam(task: PipelineBatchTask): PipelineBatchCopyTaskParam? {
        return task.taskParam?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java)
        }
    }

    private fun getParam(task: PipelineBatchTask): PipelineBatchCopyTaskParam {
        return parseParam(task) ?: throw InvalidParamException("taskParam must be PipelineBatchCopyTaskParam")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyBatchTaskHandler::class.java)
    }
}
