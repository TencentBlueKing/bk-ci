package com.tencent.devops.process.service.task.handler

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskAnalyzeEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceRel
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSummary
import com.tencent.devops.process.service.PipelineDependentResourceService
import com.tencent.devops.process.service.task.PipelineBatchTaskFactory
import com.tencent.devops.process.service.task.PipelineBatchTaskHandler
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyBatchTaskHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineDependentResourceService: PipelineDependentResourceService,
    private val pipelineBatchTaskFactory: PipelineBatchTaskFactory
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
        val details = pipelineBatchTaskDetailDao.listByTaskId(
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
        val resources = allDetails.map(::buildPipelineCopyResource)
        val relations = allDetails.map(::buildPipelineCopyResourceRel)
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
                    pacCount = allDetails.count { it.pac },
                    taskSummary = JsonUtil.toJson(buildSummary(resources), formatted = false)
                )
            )
            pipelineCopyTaskResourceDao.batchCreate(
                dslContext = transactionContext,
                resources = resources
            )
            pipelineCopyTaskResourceRelDao.batchCreate(
                dslContext = transactionContext,
                relations = relations
            )
        }
    }

    override fun handleAnalyzeEvent(event: PipelineBatchTaskAnalyzeEvent) {
        getTask(projectId = event.projectId, taskId = event.taskId) ?: return
        val changeCount = pipelineBatchTaskDetailDao.count(
            dslContext = dslContext,
            projectId = event.projectId,
            taskId = event.taskId,
            change = true
        )
        if (changeCount == 0L) {
            return
        }
        excludedSubPipelineTask(projectId = event.projectId, taskId = event.taskId)
        deleteExcludedCopyResources(projectId = event.projectId, taskId = event.taskId)
        createOrUpdateCopyResources(projectId = event.projectId, taskId = event.taskId)
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
    }

    private fun buildPipelineCopyResource(detail: PipelineBatchTaskDetail): PipelineCopyTaskResource {
        return PipelineCopyTaskResource(
            taskId = detail.taskId,
            projectId = detail.projectId,
            resourceType = PipelineDependentResourceType.PIPELINE,
            resourceId = detail.pipelineId,
            resourceName = detail.pipelineName,
            status = PipelineCopyTaskResourceStatus.PROCESSED
        )
    }

    private fun buildPipelineCopyResourceRel(detail: PipelineBatchTaskDetail): PipelineCopyTaskResourceRel {
        return PipelineCopyTaskResourceRel(
            taskId = detail.taskId,
            projectId = detail.projectId,
            pipelineId = detail.pipelineId,
            pipelineName = detail.pipelineName,
            resourceType = PipelineDependentResourceType.PIPELINE,
            resourceId = detail.pipelineId,
            resourceName = detail.pipelineName
        )
    }

    private fun buildSummary(resources: List<PipelineCopyTaskResource>): PipelineCopyTaskSummary {
        return PipelineCopyTaskSummary(
            unprocessedCount = resources.count { it.status == PipelineCopyTaskResourceStatus.UNPROCESSED },
            highRiskCount = resources.count { it.highRisk },
            autoFinishCount = resources.count { it.copyAction == PipelineCopyAction.AUTO_FINISH }
        )
    }

    /**
     * 排查子流水线任务
     *
     * 当流水线被排除后,需要把关联的子流水线任务也排除,只有当子流水线仅被当前排除的流水线引用时,才能排除
     */
    private fun excludedSubPipelineTask(
        projectId: String,
        taskId: String
    ) {
        // 获取排除的任务详情
        val taskDetails = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            subPipeline = false,
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        )
        if (taskDetails.isEmpty()) {
            return
        }
        val excludedPipelineIds = taskDetails.map { it.pipelineId }.toSet()
        // 获取排除流水线依赖的子流水线
        val resourceIds = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludedPipelineIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).map { it.resourceId }.toSet()
        if (resourceIds.isEmpty()) {
            return
        }
        // 子流水线除了被排除的流水线引用,还有没有被其他流水线引用,如果还有被其他流水线引用,则不排除
        val activeResourceCountMap = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = resourceIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).filterNot { excludedPipelineIds.contains(it.pipelineId) || it.pipelineId == it.resourceId }
            .groupingBy { it.resourceId }
            .eachCount()
        val subPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = resourceIds,
            subPipeline = true
        ).map { it.pipelineId }.toSet()
        val excludeSubPipelineIds = resourceIds.filter {
            subPipelineIds.contains(it) && (activeResourceCountMap[it] ?: 0) == 0
        }.toSet()
        if (excludeSubPipelineIds.isEmpty()) {
            return
        }
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludeSubPipelineIds,
            status = PipelineBatchTaskDetailStatus.EXCLUDED,
            change = true
        )
    }

    /**
     * 删除已排除流水线复制资源
     *
     * 删除任务中排除的流水线复制资源
     */
    private fun deleteExcludedCopyResources(
        projectId: String,
        taskId: String
    ) {
        // 获取排除的任务详情
        val taskDetails = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        )
        if (taskDetails.isEmpty()) {
            return
        }
        pipelineCopyTaskResourceRelDao.deleteByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = taskDetails.map { it.pipelineId }.toSet()
        )
    }

    /**
     * 创建或更新复制资源
     */
    private fun createOrUpdateCopyResources(
        projectId: String,
        taskId: String
    ) {

    }

    private fun getTask(projectId: String, taskId: String): PipelineBatchTask? {
        return pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: run {
            logger.warn(
                "pipeline copy batch task not found when create event received|projectId=$projectId|taskId=taskId"
            )
            return null
        }
    }

    private fun getParam(task: PipelineBatchTask): PipelineBatchCopyTaskParam {
        return task.taskParam?.let { JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java) }
            ?: throw InvalidParamException("taskParam must be PipelineBatchCopyTaskParam")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyBatchTaskHandler::class.java)
    }
}
