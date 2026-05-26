package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.Tables.T_PIPELINE_COPY_TASK_RESOURCE
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceProperties
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class PipelineCopyTaskResourceDao {

    fun batchCreate(
        dslContext: DSLContext,
        resources: List<PipelineCopyTaskResourceInfo>
    ) {
        if (resources.isEmpty()) {
            return
        }
        val queries = with(T_PIPELINE_COPY_TASK_RESOURCE) {
            resources.map { resource ->
                dslContext.insertInto(
                    this,
                    TASK_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    RESOURCE_TYPE,
                    RESOURCE_ID,
                    RESOURCE_NAME,
                    RESOURCE_PROPERTIES,
                    COPY_STRATEGY,
                    TARGET_PROJECT_ID,
                    TARGET_RESOURCE_TYPE,
                    TARGET_RESOURCE_ID,
                    TARGET_RESOURCE_NAME,
                    TARGET_RESOURCE_PROPERTIES,
                    STATUS,
                    ERROR_MESSAGE,
                    HIGH_RISK,
                    TARGET_NAME_EXISTS,
                    TARGET_ID_EXISTS,
                    AUTO_FINISH,
                    NEED_COMPLETION,
                    NEED_TRANSFER,
                    CONFIRMED
                ).values(
                    resource.taskId,
                    resource.projectId,
                    resource.pipelineId,
                    resource.resourceType.name,
                    resource.resourceId,
                    resource.resourceName,
                    resource.resourceProperties?.let { JsonUtil.toJson(it, formatted = false) },
                    resource.copyStrategy?.name,
                    resource.targetProjectId,
                    resource.targetResourceType?.name,
                    resource.targetResourceId,
                    resource.targetResourceName,
                    resource.targetResourceProperties?.let { JsonUtil.toJson(it, formatted = false) },
                    resource.status.name,
                    resource.errorMessage,
                    resource.highRisk,
                    resource.targetNameExists,
                    resource.targetIdExists,
                    resource.autoFinish,
                    resource.needCompletion,
                    resource.needTransfer,
                    resource.confirmed
                )
            }
        }
        dslContext.batch(queries).execute()
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>? = null,
        resourceIds: Set<String>? = null,
        resourceType: PipelineDependentResourceType? = null
    ): List<PipelineCopyTaskResourceInfo> {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.selectFrom(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineIds = pipelineIds,
                        resourceIds = resourceIds,
                        resourceType = resourceType
                    )
                )
                .fetch()
                .map(::convert)
        }
    }

    fun deleteByTaskId(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }

    fun deleteByPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    fun deleteByPipelineIdExcludePipelineResource(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(RESOURCE_TYPE.ne(PipelineDependentResourceType.PIPELINE.name))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        update: PipelineCopyTaskResourceUpdate
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            val query = dslContext.update(this)
            update.targetResourceType?.let { query.set(TARGET_RESOURCE_TYPE, it.name) }
            update.targetResourceId?.let { query.set(TARGET_RESOURCE_ID, it) }
            update.targetResourceName?.let { query.set(TARGET_RESOURCE_NAME, it) }
            update.targetResourceProperties?.let {
                query.set(TARGET_RESOURCE_PROPERTIES, JsonUtil.toJson(it, formatted = false))
            }
            update.status?.let { query.set(STATUS, it.name) }
            update.errorMessage?.let { query.set(ERROR_MESSAGE, it) }
            update.targetNameExists?.let { query.set(TARGET_NAME_EXISTS, it) }
            update.targetIdExists?.let { query.set(TARGET_ID_EXISTS, it) }
            update.autoFinish?.let { query.set(AUTO_FINISH, it) }
            update.needCompletion?.let { query.set(NEED_COMPLETION, it) }
            update.needTransfer?.let { query.set(NEED_TRANSFER, it) }
            update.confirmed?.let { query.set(CONFIRMED, it) }
            query.where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .and(RESOURCE_TYPE.eq(update.resourceType.name))
                .and(RESOURCE_ID.eq(update.resourceId))
                .execute()
        }
    }

    private fun convert(record: Record): PipelineCopyTaskResourceInfo {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            PipelineCopyTaskResourceInfo(
                taskId = record.get(TASK_ID),
                projectId = record.get(PROJECT_ID),
                pipelineId = record.get(PIPELINE_ID),
                resourceType = PipelineDependentResourceType.valueOf(record.get(RESOURCE_TYPE)),
                resourceId = record.get(RESOURCE_ID),
                resourceName = record.get(RESOURCE_NAME),
                resourceProperties = toProperties(record.get(RESOURCE_PROPERTIES)),
                copyStrategy = record.get(COPY_STRATEGY)?.let { PipelineCopyStrategy.valueOf(it) },
                targetProjectId = record.get(TARGET_PROJECT_ID),
                targetResourceType = record.get(TARGET_RESOURCE_TYPE)?.let {
                    PipelineDependentResourceType.valueOf(it)
                },
                targetResourceId = record.get(TARGET_RESOURCE_ID),
                targetResourceName = record.get(TARGET_RESOURCE_NAME),
                targetResourceProperties = toProperties(record.get(TARGET_RESOURCE_PROPERTIES)),
                status = PipelineCopyTaskResourceStatus.valueOf(record.get(STATUS)),
                errorMessage = record.get(ERROR_MESSAGE),
                highRisk = record.get(HIGH_RISK),
                targetNameExists = record.get(TARGET_NAME_EXISTS),
                targetIdExists = record.get(TARGET_ID_EXISTS),
                autoFinish = record.get(AUTO_FINISH),
                needCompletion = record.get(NEED_COMPLETION),
                needTransfer = record.get(NEED_TRANSFER),
                confirmed = record.get(CONFIRMED),
                createTime = record.get(CREATE_TIME),
                updateTime = record.get(UPDATE_TIME)
            )
        }
    }

    private fun buildConditions(
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>?,
        resourceIds: Set<String>?,
        resourceType: PipelineDependentResourceType?
    ): List<Condition> {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(TASK_ID.eq(taskId))
            if (!pipelineIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(pipelineIds))
            }
            if (!resourceIds.isNullOrEmpty()) {
                conditions.add(RESOURCE_ID.`in`(resourceIds))
            }
            if (resourceType != null) {
                conditions.add(RESOURCE_TYPE.eq(resourceType.name))
            }
            conditions
        }
    }

    private fun toProperties(value: String?): PipelineCopyResourceProperties? {
        return value?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineCopyResourceProperties::class.java)
        }
    }
}
