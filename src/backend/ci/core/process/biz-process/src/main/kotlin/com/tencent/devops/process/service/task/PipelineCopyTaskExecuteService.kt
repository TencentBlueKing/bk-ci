package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制任务执行服务
 */
@Service
class PipelineCopyTaskExecuteService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
) {

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        with(event) {
            tryStartExecute(projectId = projectId, taskId = taskId) ?: return
            val resources = pipelineCopyTaskResourceDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineCopyTaskResourceStatus.PROCESSED
            )
            if (resources.isNotEmpty()) {
                // 凭证要在代码库之前创建
                executeCredentials(projectId = projectId, taskId = taskId, resources = resources)
                executeRepositories(projectId = projectId, taskId = taskId, resources = resources)
                // 节点要在环境之前创建
                executeNodes(projectId = projectId, taskId = taskId, resources = resources)
                executeEnvs(projectId = projectId, taskId = taskId, resources = resources)
                executePipelineGroups(projectId = projectId, taskId = taskId, resources = resources)
                executePipelineLabels(projectId = projectId, taskId = taskId, resources = resources)
                executePipelineTemplates(projectId = projectId, taskId = taskId, resources = resources)
                executePipelines(projectId = projectId, taskId = taskId, resources = resources)
            }
            finishExecute(projectId = projectId, taskId = taskId)
        }
    }

    private fun tryStartExecute(
        projectId: String,
        taskId: String
    ): PipelineBatchTask? {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        try {
            lock.lock()
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
            if (task.status != PipelineBatchTaskStatus.DRAFT) {
                logger.warn("pipeline batch task status not match|$projectId|$taskId|${task.status}")
                return null
            }
            val unprocessedCount = pipelineCopyTaskResourceDao.count(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineCopyTaskResourceStatus.UNPROCESSED
            )
            if (unprocessedCount > 0) {
                logger.warn("pipeline copy task has unprocessed resources|$projectId|$taskId|$unprocessedCount")
                return null
            }
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.EXECUTING
            )
            return task
        } finally {
            lock.unlock()
        }
    }

    private fun executeCredentials(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.CREDENTIAL
        }.forEach {
            executeCredential(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executeCredential(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET -> {

            }

            PipelineCopyStrategy.CREDENTIAL_CREATE_NEW -> {

            }

            else -> {
                logger.error("unknown credential copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executeRepositories(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.REPOSITORY
        }.forEach {
            executeRepository(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executeRepository(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL -> {

            }

            PipelineCopyStrategy.REPOSITORY_CREATE_NEW -> {

            }

            else -> {
                logger.error("unknown repository copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executeEnvs(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_ENV ||
                it.resourceType == PipelineDependentResourceType.DEPLOY_ENV
        }.forEach {
            executeEnv(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executeEnv(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.BUILD_ENV_CREATE_WITHOUT_NODE -> {

            }

            PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE -> {

            }

            PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.DEPLOY_ENV_CREATE_WITHOUT_NODE -> {

            }

            PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE -> {

            }

            else -> {
                logger.error("unknown env copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executeNodes(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_NODE ||
                it.resourceType == PipelineDependentResourceType.DEPLOY_NODE
        }.forEach {
            executeNode(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executeNode(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT -> {

            }

            PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT -> {

            }

            else -> {
                logger.error("unknown node copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executePipelineGroups(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_GROUP
        }.forEach {
            executePipelineGroup(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executePipelineGroup(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE -> {

            }

            PipelineCopyStrategy.PIPELINE_GROUP_IGNORE -> {

            }

            else -> {
                logger.error("unknown pipeline group copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executePipelineLabels(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_LABEL
        }.forEach {
            executePipelineLabel(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executePipelineLabel(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE -> {

            }

            PipelineCopyStrategy.LABEL_IGNORE -> {

            }

            else -> {
                logger.error("unknown pipeline label copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executePipelineTemplates(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_TEMPLATE
        }.forEach {
            executePipelineTemplate(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executePipelineTemplate(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.PIPELINE_TEMPLATE_CREATE_NEW -> {

            }

            else -> {
                logger.error("unknown pipeline template copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executePipelines(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE
        }.forEach {
            executePipeline(projectId = projectId, taskId = taskId, it)
        }
    }

    private fun executePipeline(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.PIPELINE_CREATE_NEW_ID -> {

            }

            PipelineCopyStrategy.PIPELINE_REUSE_SOURCE_ID -> {

            }

            PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT -> {

            }

            PipelineCopyStrategy.PIPELINE_SKIP -> {

            }

            else -> {
                logger.error("unknown pipeline copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun finishExecute(
        projectId: String,
        taskId: String
    ) {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        try {
            lock.lock()
            val resources = pipelineCopyTaskResourceDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId
            )
            val successCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.SUCCESS
            }
            val failedCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.FAILED
            }
            pipelineBatchTaskDao.update(
                dslContext = dslContext,
                update = PipelineBatchTaskUpdate(
                    projectId = projectId,
                    taskId = taskId,
                    taskSummary = JsonUtil.toJson(buildSummary(resources), formatted = false),
                    status = buildExecuteStatus(
                        resourceCount = resources.size,
                        successCount = successCount,
                        failedCount = failedCount
                    ),
                    successCount = successCount,
                    failedCount = failedCount
                )
            )
        } finally {
            lock.unlock()
        }
    }

    private fun buildSummary(resources: List<PipelineCopyTaskResource>): PipelineCopyTaskSummary {
        return PipelineCopyTaskSummary(
            unprocessedCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.UNPROCESSED
            },
            highRiskCount = resources.count { it.highRisk },
            needCompletionCount = resources.count {
                it.copyAction == PipelineCopyAction.NEED_COMPLETION
            },
            needTransferCount = resources.count {
                it.copyAction == PipelineCopyAction.NEED_TRANSFER
            },
            autoFinishCount = resources.count {
                it.copyAction == PipelineCopyAction.AUTO_FINISH
            }
        )
    }

    private fun buildExecuteStatus(
        resourceCount: Int,
        successCount: Int,
        failedCount: Int
    ): PipelineBatchTaskStatus {
        val resultCount = successCount + failedCount
        return when {
            resultCount == 0 -> PipelineBatchTaskStatus.DRAFT
            failedCount == 0 && successCount == resourceCount -> PipelineBatchTaskStatus.SUCCESS
            successCount == 0 -> PipelineBatchTaskStatus.FAILED
            else -> PipelineBatchTaskStatus.PARTIAL_FAILED
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskExecuteService::class.java)
    }
}
