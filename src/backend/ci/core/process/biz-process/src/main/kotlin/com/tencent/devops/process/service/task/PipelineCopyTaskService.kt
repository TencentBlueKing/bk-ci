package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyPipelineInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceInfoGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteProgress
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSummary
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao,
    private val pipelinePermissionService: PipelinePermissionService,
) {

    fun get(projectId: String, taskId: String): PipelineCopyTaskInfo? {
        val task = getTask(projectId = projectId, taskId = taskId)
        val param = parseParam(task)
        val summary = parseSummary(task)
        return PipelineCopyTaskInfo(
            taskId = task.taskId,
            projectId = task.projectId,
            taskName = task.taskName,
            targetProjectId = param?.targetProjectId,
            pipelineCopyStrategy = param?.pipelineCopyStrategy,
            status = task.status,
            pipelineCount = task.totalCount,
            subPipelineCount = task.subPipelineCount,
            pacCount = task.pacCount,
            unprocessedCount = summary.unprocessedCount,
            highRiskCount = summary.highRiskCount,
            autoFinishCount = summary.autoFinishCount,
            creator = task.creator,
            createTime = task.createTime,
            updateTime = task.updateTime
        )
    }

    fun saveConfigDraft(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskConfigRequest
    ): Boolean {
        val targetProjectId = request.targetProjectId
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = targetProjectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, targetProjectId)
            )
        }
        val task = getTask(projectId = projectId, taskId = taskId)
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_SAVE_CONFIG,
                params = arrayOf(taskId, task.status.name)
            )
        }
        val param = PipelineBatchCopyTaskParam(
            targetProjectId = targetProjectId,
            pipelineCopyStrategy = request.pipelineCopyStrategy
        )
        return pipelineBatchTaskDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskUpdate(
                projectId = projectId,
                taskId = taskId,
                taskName = request.taskName,
                taskParam = JsonUtil.toJson(param, formatted = false)
            )
        ) == 1
    }

    fun listResource(
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType? = null,
        resourceName: String? = null,
        copyAction: PipelineCopyAction? = null
    ): List<PipelineCopyResourceInfoGroup> {
        getTask(projectId = projectId, taskId = taskId)
        return pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceType = resourceType,
            resourceName = resourceName,
            copyAction = copyAction
        ).groupBy { it.resourceType }
            .map { (type, resources) ->
                PipelineCopyResourceInfoGroup(
                    resourceType = type,
                    totalCount = resources.size.toLong(),
                    sourceProjectReferCount = 0,
                    resources = resources
                )
            }
    }

    fun listResourcePipelines(
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        pipelineName: String?
    ): List<PipelineCopyPipelineInfo> {
        getTask(projectId = projectId, taskId = taskId)
        return pipelineCopyTaskResourceRelDao.listResourcePipelines(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceType = resourceType,
            resourceId = resourceId,
            pipelineName = pipelineName
        )
    }

    fun saveResourceDraft(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResourceInfo>
    ): Boolean {
        getTask(projectId = projectId, taskId = taskId)
        var success = true
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            resources.forEach { resource ->
                val copyStrategy = requireNotNull(resource.copyStrategy) {
                    "copyStrategy cannot be null when save resource draft"
                }
                val updateCount = pipelineCopyTaskResourceDao.updateDraft(
                    dslContext = transactionContext,
                    update = PipelineCopyTaskResourceUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        resourceType = resource.resourceType,
                        resourceId = resource.resourceId,
                        copyStrategy = copyStrategy,
                        targetResourceProperties = resource.targetResourceProperties,
                        copyAction = copyStrategy.copyAction,
                        status = PipelineCopyTaskResourceStatus.PROCESSED
                    )
                )
                success = success && updateCount == 1
            }
            refreshSummary(
                dslContext = transactionContext,
                projectId = projectId,
                taskId = taskId
            )
        }
        return success
    }

    fun prepareExecute(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResourceInfo>
    ): Boolean {
        val saveResult = saveResourceDraft(
            projectId = projectId,
            taskId = taskId,
            resources = resources
        )
        val unprocessedCount = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ).count { it.status == PipelineCopyTaskResourceStatus.UNPROCESSED }
        if (unprocessedCount > 0) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_NOT_ALL_PROCESSED,
                params = arrayOf(taskId, unprocessedCount.toString())
            )
        }
        return saveResult
    }

    fun executeSummary(
        projectId: String,
        taskId: String
    ): PipelineCopyTaskExecuteSummary {
        val task = getTask(projectId = projectId, taskId = taskId)
        val resources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        return PipelineCopyTaskExecuteSummary(
            pipelineCount = task.totalCount.toLong(),
            needCompletionCount = resources.count { it.copyAction == PipelineCopyAction.NEED_COMPLETION }.toLong(),
            needTransferCount = resources.count { it.copyAction == PipelineCopyAction.NEED_TRANSFER }.toLong(),
            autoFinishCount = resources.count { it.copyAction == PipelineCopyAction.AUTO_FINISH }.toLong()
        )
    }

    fun executeProgress(
        projectId: String,
        taskId: String
    ): PipelineCopyTaskExecuteProgress {
        val task = getTask(projectId = projectId, taskId = taskId)
        return PipelineCopyTaskExecuteProgress(
            status = task.status,
            totalCount = task.totalCount.toLong(),
            executedCount = executedCount(task)
        )
    }

    fun confirmResource(
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        confirmed: Boolean
    ): Boolean {
        getTask(projectId = projectId, taskId = taskId)
        return pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
                projectId = projectId,
                taskId = taskId,
                resourceType = resourceType,
                resourceId = resourceId,
                confirmed = confirmed
            )
        ) == 1
    }

    private fun getTask(projectId: String, taskId: String): PipelineBatchTaskInfo {
        val task = pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        if (task.taskType != PipelineBatchTaskType.PIPELINE_COPY) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_TYPE_NOT_MATCH,
                params = arrayOf(taskId, task.taskType.name, PipelineBatchTaskType.PIPELINE_COPY.name)
            )
        }
        return task
    }

    private fun parseParam(task: PipelineBatchTaskInfo): PipelineBatchCopyTaskParam? {
        return task.taskParam?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java)
        }
    }

    private fun parseSummary(task: PipelineBatchTaskInfo): PipelineCopyTaskSummary {
        return task.taskSummary?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineCopyTaskSummary::class.java)
        } ?: PipelineCopyTaskSummary()
    }

    private fun refreshSummary(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ) {
        val resources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        pipelineBatchTaskDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskUpdate(
                projectId = projectId,
                taskId = taskId,
                taskSummary = JsonUtil.toJson(
                    PipelineCopyTaskSummary(
                        unprocessedCount = resources.count { it.status == PipelineCopyTaskResourceStatus.UNPROCESSED },
                        highRiskCount = resources.count { it.highRisk },
                        autoFinishCount = resources.count { it.copyAction == PipelineCopyAction.AUTO_FINISH }
                    ),
                    formatted = false
                )
            )
        )
    }

    private fun executedCount(task: PipelineBatchTaskInfo): Long {
        val summaries = pipelineBatchTaskDetailDao.detailStatusSummary(
            dslContext = dslContext,
            projectId = task.projectId,
            taskId = task.taskId,
            taskType = task.taskType
        )
        return summaries.filter {
            it.status == PipelineBatchTaskDetailStatus.SUCCESS || it.status == PipelineBatchTaskDetailStatus.FAILED
        }.sumOf { it.count }
    }
}
