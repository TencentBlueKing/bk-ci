package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskLabelSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStep
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBatchTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val handlers: List<PipelineBatchTaskHandler>,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService
) {

    fun list(
        projectId: String,
        type: PipelineBatchTaskType?,
        status: PipelineBatchTaskStatus?,
        creator: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineBatchTaskInfo> {
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val count = pipelineBatchTaskDao.count(
            dslContext = dslContext,
            projectId = projectId,
            type = type,
            status = status,
            creator = creator
        )
        val records = pipelineBatchTaskDao.list(
            dslContext = dslContext,
            projectId = projectId,
            type = type,
            status = status,
            creator = creator,
            offset = offset,
            limit = limit
        )
        return SQLPage(count = count, records = records)
    }

    fun create(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ): String {
        validateCreateRequest(request)
        val handler = getHandler(taskType = request.taskType)
        handler.validateWhenCreate(
            userId = userId,
            projectId = projectId,
            request = request
        )
        val pipelineIds = request.pipelineIds.distinct()
        val pipelineInfos = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds.toSet()
        )
        val pipelineNameMap = pipelineInfos.associate { it.pipelineId to it.pipelineName }
        val pipelineLockedMap = pipelineInfos.associate { it.pipelineId to (it.locked ?: false) }
        val pipelineEnablePacSet = pipelineYamlInfoDao.listByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).map { it.pipelineId }.toSet()
        val pipelineConstraintSet = pipelineTemplateRelatedService.listByPipelineIds(
            projectId = projectId,
            pipelineIds = pipelineIds.toSet()
        ).filter { it.instanceType == PipelineInstanceTypeEnum.CONSTRAINT }
            .map { it.pipelineId }
            .toSet()
        val missingPipelineIds = pipelineIds.filterNot { pipelineNameMap.containsKey(it) }
        if (missingPipelineIds.isNotEmpty()) {
            throw InvalidParamException("Pipeline not found: ${missingPipelineIds.joinToString(",")}")
        }
        val taskId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBatchTaskDao.create(
                dslContext = transactionContext,
                taskId = taskId,
                projectId = projectId,
                taskName = request.taskName,
                taskType = request.taskType,
                taskParam = null,
                status = handler.taskStatusWhenCreate(),
                step = PipelineBatchTaskStep.CONFIG,
                totalCount = pipelineIds.size,
                creator = userId
            )
            pipelineBatchTaskDetailDao.batchCreate(
                dslContext = transactionContext,
                details = pipelineIds.map { pipelineId ->
                    PipelineBatchTaskDetailInfo(
                        taskId = taskId,
                        projectId = projectId,
                        taskType = request.taskType,
                        pipelineId = pipelineId,
                        pipelineName = pipelineNameMap[pipelineId].orEmpty(),
                        pac = pipelineEnablePacSet.contains(pipelineId),
                        constraint = pipelineConstraintSet.contains(pipelineId),
                        subPipeline = false,
                        locked = pipelineLockedMap[pipelineId] ?: false,
                        status = handler.detailStatusWhenCreate(),
                        errorMessage = null,
                        startTime = null,
                        endTime = null
                    )
                }
            )
        }
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskCreateEvent(
                taskId = taskId,
                taskType = request.taskType,
                projectId = projectId
            )
        )
        return taskId
    }

    fun get(projectId: String, taskId: String): PipelineBatchTaskInfo? {
        return pipelineBatchTaskDao.get(dslContext = dslContext, projectId = projectId, taskId = taskId)
    }

    fun listDetails(
        projectId: String,
        taskId: String,
        pipelineName: String?,
        status: PipelineBatchTaskDetailStatus?,
        pac: Boolean?,
        subPipeline: Boolean?,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineBatchTaskDetailInfo> {
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val count = pipelineBatchTaskDetailDao.count(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineName = pipelineName,
            status = status,
            pac = pac,
            subPipeline = subPipeline
        )
        val records = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineName = pipelineName,
            status = status,
            pac = pac,
            subPipeline = subPipeline,
            offset = offset,
            limit = limit
        )
        return SQLPage(count = count, records = records)
    }

    fun detailStatusSummary(
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType
    ): List<PipelineBatchTaskDetailStatusSummary> {
        return pipelineBatchTaskDetailDao.detailStatusSummary(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            taskType = taskType
        )
    }

    fun taskLabelSummary(
        projectId: String,
        taskId: String
    ): PipelineBatchTaskLabelSummary {
        return PipelineBatchTaskLabelSummary(
            pacCount = pipelineBatchTaskDetailDao.count(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                pac = true
            ),
            subPipelineCount = pipelineBatchTaskDetailDao.count(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                subPipeline = true
            )
        )
    }

    fun config(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineBatchTaskConfigRequest
    ) {
        validateConfigRequest(request)
        val task = getTask(projectId = projectId, taskId = taskId)
        val handler = getHandler(taskType = task.taskType)
        handler.validateWhenConfig(
            userId = userId,
            projectId = projectId,
            task = task,
            request = request
        )
        val taskParam = request.taskParam?.let { JsonUtil.toJson(it, formatted = false) }
        pipelineBatchTaskDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskUpdate(
                projectId = projectId,
                taskId = taskId,
                taskName = request.taskName,
                taskParam = taskParam
            )
        )
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskConfigEvent(
                taskId = task.taskId,
                taskType = task.taskType,
                projectId = task.projectId
            )
        )
    }

    fun excludePipeline(
        projectId: String,
        taskId: String,
        pipelineId: String
    ) {
        val detail = getTaskDetail(projectId = projectId, taskId = taskId, pipelineId = pipelineId)
        if (detail.subPipeline || detail.status in DETAIL_STATUS_CAN_NOT_EXCLUDE) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_EXCLUDE,
                params = arrayOf(pipelineId, detail.status.desc)
            )
        }
        pipelineBatchTaskDetailDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskDetailUpdate(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId,
                status = PipelineBatchTaskDetailStatus.EXCLUDED,
                change = true
            )
        )
    }

    fun restorePipeline(
        projectId: String,
        taskId: String,
        pipelineId: String
    ) {
        val detail = getTaskDetail(projectId = projectId, taskId = taskId, pipelineId = pipelineId)
        if (detail.status != PipelineBatchTaskDetailStatus.EXCLUDED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_RESTORE,
                params = arrayOf(pipelineId, detail.status.desc)
            )
        }
        pipelineBatchTaskDetailDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskDetailUpdate(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId,
                status = PipelineBatchTaskDetailStatus.WAIT_COPY,
                change = true
            )
        )
    }

    fun execute(
        userId: String,
        projectId: String,
        taskId: String
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_EXECUTE,
                params = arrayOf(taskId, task.status.desc)
            )
        }
        val handler = getHandler(taskType = task.taskType)
        handler.validateWhenExecute(
            userId = userId,
            projectId = projectId,
            task = task
        )
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskExecuteEvent(
                taskId = task.taskId,
                taskType = task.taskType,
                projectId = task.projectId
            )
        )
    }

    fun delete(
        userId: String,
        projectId: String,
        taskId: String
    ): Boolean {
        val task = pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_DELETE,
                params = arrayOf(taskId, task.status.desc)
            )
        }
        getHandler(taskType = task.taskType).validateWhenDelete(
            userId = userId,
            projectId = projectId,
            task = task
        )
        return pipelineBatchTaskDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskUpdate(
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.DELETED
            )
        ) == 1
    }

    fun retry(projectId: String, taskId: String): Boolean {
        val task = pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: return false
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskExecuteEvent(
                taskId = task.taskId,
                taskType = task.taskType,
                projectId = task.projectId
            )
        )
        return true
    }

    private fun validateCreateRequest(request: PipelineBatchTaskCreateRequest) {
        if (request.pipelineIds.isEmpty()) {
            throw InvalidParamException("pipelineIds cannot be empty")
        }
    }

    private fun validateConfigRequest(request: PipelineBatchTaskConfigRequest) {
        if (request.taskName.isBlank()) {
            throw InvalidParamException("taskName cannot be blank")
        }
    }

    private fun getTask(projectId: String, taskId: String): PipelineBatchTaskInfo {
        return pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOT_EXISTS,
            params = arrayOf(taskId)
        )
    }

    private fun getTaskDetail(
        projectId: String,
        taskId: String,
        pipelineId: String
    ): PipelineBatchTaskDetailInfo {
        return pipelineBatchTaskDetailDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
    }

    private fun getHandler(taskType: PipelineBatchTaskType): PipelineBatchTaskHandler {
        return handlers.singleOrNull { it.support(taskType) }
            ?: throw InvalidParamException("unsupported taskType: $taskType")
    }

    companion object {
        private val DETAIL_STATUS_CAN_NOT_EXCLUDE = setOf(
            PipelineBatchTaskDetailStatus.EXCLUDED,
            PipelineBatchTaskDetailStatus.SUCCESS,
            PipelineBatchTaskDetailStatus.FAILED
        )
    }
}
