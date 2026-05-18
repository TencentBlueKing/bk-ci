package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
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
    private val pipelinePermissionService: PipelinePermissionService,
    private val sampleEventDispatcher: SampleEventDispatcher
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
        val pipelineIds = request.pipelineIds.distinct()
        pipelineIds.forEach { pipelineId ->
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE,
                message = null
            )
        }
        val pipelineInfos = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds.toSet()
        )
        val pipelineNameMap = pipelineInfos.associate { it.pipelineId to it.pipelineName }
        val missingPipelineIds = pipelineIds.filterNot { pipelineNameMap.containsKey(it) }
        if (missingPipelineIds.isNotEmpty()) {
            throw InvalidParamException("Pipeline not found: ${missingPipelineIds.joinToString(",")}")
        }
        val taskId = UUIDUtil.generate()
        val taskParam = request.taskParam?.let { JsonUtil.toJson(it, formatted = false) }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBatchTaskDao.create(
                dslContext = transactionContext,
                taskId = taskId,
                projectId = projectId,
                taskName = request.taskName,
                taskType = request.taskType,
                taskParam = taskParam,
                totalCount = pipelineIds.size,
                creator = userId
            )
            pipelineBatchTaskDetailDao.batchCreate(
                dslContext = transactionContext,
                details = pipelineIds.map { pipelineId ->
                    PipelineBatchTaskDetailInfo(
                        taskId = taskId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineName = pipelineNameMap[pipelineId].orEmpty(),
                        status = PipelineBatchTaskStatus.DRAFT,
                        errorMessage = null,
                        startTime = null,
                        endTime = null
                    )
                }
            )
        }
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskExecuteEvent(
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
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineBatchTaskDetailInfo> {
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val count = pipelineBatchTaskDetailDao.count(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        val records = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            offset = offset,
            limit = limit
        )
        return SQLPage(count = count, records = records)
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
        if (request.taskName.isBlank()) {
            throw InvalidParamException("taskName cannot be blank")
        }
        if (request.pipelineIds.isEmpty()) {
            throw InvalidParamException("pipelineIds cannot be empty")
        }
    }
}
