package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.pojo.PipelineInfoQueryCondition
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskAnalyzeEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyPipelineInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteProgress
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSaveResourceRequest
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val authProjectApi: AuthProjectApi,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val pipelineCopyTaskSaveService: PipelineCopyTaskSaveService
) {

    fun get(userId: String, projectId: String, taskId: String): PipelineCopyTask? {
        val task = getTask(projectId = projectId, taskId = taskId)
        val param = PipelineCopyTaskUtils.parseParam(task)
        val summary = PipelineCopyTaskUtils.parseSummary(task)
        return PipelineCopyTask(
            taskId = task.taskId,
            projectId = task.projectId,
            taskName = task.taskName,
            targetProjectId = param?.targetProjectId,
            pipelineCopyStrategy = param?.pipelineCopyStrategy,
            status = task.status,
            errorMessage = task.errorMessage,
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
    ) {
        val targetProjectId = request.targetProjectId
        checkProjectManager(userId = userId, projectId = projectId)
        checkProjectManager(userId = userId, projectId = targetProjectId)
        pipelineCopyTaskSaveService.saveConfigDraft(
            projectId = projectId,
            taskId = taskId,
            request = request
        )
    }

    fun analyzeResourceDepend(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskConfigRequest
    ): Boolean {
        saveConfigDraft(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            request = request
        )
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskAnalyzeEvent(
                taskId = taskId,
                taskType = PipelineBatchTaskType.PIPELINE_COPY,
                projectId = projectId
            )
        )
        return true
    }

    fun listResource(
        userId: String,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType? = null,
        resourceName: String? = null,
        copyAction: PipelineCopyAction? = null
    ): List<PipelineCopyResourceGroup> {
        val task = getTask(projectId = projectId, taskId = taskId)
        val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        checkProjectVisit(userId = userId, projectId = projectId)
        checkProjectVisit(userId = userId, projectId = param.targetProjectId)
        return pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceType = resourceType,
            resourceName = resourceName,
            copyAction = copyAction
        ).filterNot {
            // 流水线只需要展示冲突的资源
            it.resourceType == PipelineDependentResourceType.PIPELINE &&
                !it.targetNameExists &&
                !it.targetIdExists
        }.groupBy { it.resourceType }
            .map { (type, resources) ->
                PipelineCopyResourceGroup(
                    resourceType = type,
                    totalCount = resources.size,
                    unprocessedCount = resources.count {
                        it.status == PipelineCopyTaskResourceStatus.UNPROCESSED
                    },
                    resources = resources
                )
            }
    }

    fun listResourcePipelines(
        userId: String,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        pipelineName: String?,
        locked: Boolean?,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineCopyPipelineInfo> {
        val task = getTask(projectId = projectId, taskId = taskId)
        val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        checkProjectVisit(userId = userId, projectId = projectId)
        checkProjectVisit(userId = userId, projectId = param.targetProjectId)
        val pipelineIds = pipelineCopyTaskResourceRelDao.listResourcePipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceType = resourceType,
            resourceId = resourceId
        )
        if (pipelineIds.isEmpty()) {
            return SQLPage(count = 0, records = emptyList())
        }
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val condition = PipelineInfoQueryCondition(
            projectId = projectId,
            pipelineIds = pipelineIds,
            pipelineName = pipelineName,
            locked = locked,
            offset = offset,
            limit = limit
        )
        val count = pipelineInfoDao.countByCondition(dslContext = dslContext, condition = condition)
        val records = pipelineInfoDao.listByCondition(dslContext = dslContext, condition = condition)
            .mapNotNull { pipelineInfoDao.convert(it, templateId = null) }
            .map {
                PipelineCopyPipelineInfo(
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    locked = it.locked ?: false
                )
            }
        return SQLPage(count = count, records = records)
    }

    fun saveResourceDraft(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskSaveResourceRequest
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        checkProjectManager(userId = userId, projectId = projectId)
        checkProjectManager(userId = userId, projectId = param.targetProjectId)
        pipelineCopyTaskSaveService.saveResourceDraft(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            request = request
        )
    }

    fun prepareExecute(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskSaveResourceRequest
    ) {
        val unprocessedResources = request.resources
            .filter {
                it.resourceType != PipelineDependentResourceType.PIPELINE_GROUP &&
                    it.resourceType != PipelineDependentResourceType.PIPELINE_LABEL
            }
            .filter { request.getCopyStrategy(it) == null }
            .map { it.resourceName }
        if (unprocessedResources.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_EMPTY,
                params = arrayOf(taskId, unprocessedResources.joinToString(","))
            )
        }
        saveResourceDraft(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            request = request
        )
        val unProcessedCount = pipelineCopyTaskResourceDao.count(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED
        )
        if (unProcessedCount > 0) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_NOT_ALL_PROCESSED,
                params = arrayOf(taskId, unProcessedCount.toString())
            )
        }
    }

    fun execute(
        userId: String,
        projectId: String,
        taskId: String
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        if (task.status !in setOf(
                PipelineBatchTaskStatus.DRAFT,
                PipelineBatchTaskStatus.EXECUTE_FAILED
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_EXECUTE,
                params = arrayOf(taskId, task.status.name)
            )
        }
        val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        checkProjectManager(userId = userId, projectId = projectId)
        checkProjectManager(userId = userId, projectId = param.targetProjectId)

        val unprocessedCount = pipelineCopyTaskResourceDao.count(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED
        )
        if (unprocessedCount > 0) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_NOT_ALL_PROCESSED,
                params = arrayOf(taskId, unprocessedCount.toString())
            )
        }
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskExecuteEvent(
                taskId = taskId,
                taskType = PipelineBatchTaskType.PIPELINE_COPY,
                projectId = projectId
            )
        )
    }

    fun executeProgress(
        projectId: String,
        taskId: String
    ): PipelineCopyTaskExecuteProgress {
        val task = getTask(projectId = projectId, taskId = taskId)
        return PipelineCopyTaskExecuteProgress(
            status = task.status,
            totalCount = task.totalCount,
            executedCount = task.successCount + task.failedCount,
            errorMessage = task.errorMessage
        )
    }

    fun executeSummary(
        projectId: String,
        taskId: String
    ): PipelineCopyTaskExecuteSummary {
        val task = getTask(projectId = projectId, taskId = taskId)
        val summary = PipelineCopyTaskUtils.parseSummary(task)

        return PipelineCopyTaskExecuteSummary(
            pipelineCount = task.totalCount,
            needCompletionCount = summary.needCompletionCount,
            needTransferCount = summary.needTransferCount,
            autoFinishCount = summary.autoFinishCount
        )
    }

    fun confirmResource(
        userId: String,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        confirmed: Boolean
    ): Boolean {
        val task = getTask(projectId = projectId, taskId = taskId)
        checkProjectManager(userId = userId, projectId = projectId)
        val resource = pipelineCopyTaskResourceDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = task.taskId,
            resourceType = resourceType,
            resourceId = resourceId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_NOT_EXISTS,
            params = arrayOf(taskId, "${resourceType.name}:$resourceId")
        )
        if (resource.copyAction == PipelineCopyAction.AUTO_FINISH) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_AUTO_FINISH_CAN_NOT_CONFIRM,
                params = arrayOf(resource.resourceType.name, resource.resourceId)
            )
        }
        if (resource.status != PipelineCopyTaskResourceStatus.SUCCESS) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STATUS_CAN_NOT_CONFIRM,
                params = arrayOf(resource.resourceType.name, resource.resourceId, resource.status.name)
            )
        }
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

    private fun getTask(projectId: String, taskId: String): PipelineBatchTask {
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

    private fun checkProjectVisit(
        userId: String,
        projectId: String
    ) {
        if (!authProjectApi.validateUserProjectPermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                projectCode = projectId,
                permission = AuthPermission.VISIT
            )
        ) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.USER_NOT_HAVE_PROJECT_PERMISSIONS,
                params = arrayOf(userId, projectId)
            )
        }
    }

    private fun checkProjectManager(
        userId: String,
        projectId: String
    ) {
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, projectId)
            )
        }
    }
}
