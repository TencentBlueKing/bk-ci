package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_COPY_TASK_RESOURCE
import com.tencent.devops.model.process.tables.records.TPipelineCopyTaskResourceRecord
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceProp
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineCopyTaskResourceDao {

    fun batchCreate(
        dslContext: DSLContext,
        resources: List<PipelineCopyTaskResource>
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
                    COPY_ACTION,
                    CONFIRMED,
                    PIPELINE_REFER_COUNT
                ).values(
                    resource.taskId,
                    resource.projectId,
                    resource.resourceType.name,
                    resource.resourceId,
                    resource.resourceName,
                    resource.resourceProperties?.let { JsonUtil.toJson(it, formatted = false) },
                    resource.copyStrategy?.name,
                    resource.targetProjectId,
                    resource.targetResourceType?.name,
                    resource.targetResourceId,
                    resource.targetResourceName,
                    resource.targetResourceProp?.let { JsonUtil.toJson(it, formatted = false) },
                    resource.status.name,
                    resource.errorMessage,
                    resource.highRisk,
                    resource.targetNameExists,
                    resource.targetIdExists,
                    resource.copyAction.name,
                    resource.confirmed,
                    resource.pipelineReferCount
                ).onDuplicateKeyIgnore()
            }
        }
        dslContext.batch(queries).execute()
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        resourceIds: Set<String>? = null,
        resourceType: PipelineDependentResourceType? = null,
        resourceName: String? = null,
        copyAction: PipelineCopyAction? = null,
        status: PipelineCopyTaskResourceStatus? = null
    ): List<PipelineCopyTaskResource> {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.selectFrom(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        taskId = taskId,
                        resourceIds = resourceIds,
                        resourceType = resourceType,
                        resourceName = resourceName,
                        copyAction = copyAction,
                        status = status
                    )
                )
                .orderBy(RESOURCE_TYPE.asc(), RESOURCE_NAME.asc(), RESOURCE_ID.asc())
                .fetch()
                .map(::convert)
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        resourceIds: Set<String>? = null,
        resourceType: PipelineDependentResourceType? = null,
        resourceName: String? = null,
        copyAction: PipelineCopyAction? = null,
        status: PipelineCopyTaskResourceStatus? = null
    ): Long {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.selectCount()
                .from(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        taskId = taskId,
                        resourceIds = resourceIds,
                        resourceType = resourceType,
                        resourceName = resourceName,
                        copyAction = copyAction,
                        status = status
                    )
                )
                .fetchOne(0, Long::class.java) ?: 0L
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

    fun deleteByResourceIds(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceIds: Set<String>
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(RESOURCE_TYPE.eq(resourceType.name))
                .and(RESOURCE_ID.`in`(resourceIds))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        update: PipelineCopyTaskResourceUpdate
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            val query = dslContext.update(this)
            update.copyStrategy?.let { query.set(COPY_STRATEGY, it.name) }
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
            update.highRisk?.let { query.set(HIGH_RISK, it) }
            update.copyAction?.let { query.set(COPY_ACTION, it.name) }
            update.confirmed?.let { query.set(CONFIRMED, it) }
            query.set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .and(RESOURCE_TYPE.eq(update.resourceType.name))
                .and(RESOURCE_ID.eq(update.resourceId))
                .execute()
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        updates: List<PipelineCopyTaskResourceUpdate>
    ): Int {
        if (updates.isEmpty()) {
            return 0
        }
        val queries = with(T_PIPELINE_COPY_TASK_RESOURCE) {
            updates.map { resourceUpdate ->
                val query = dslContext.update(this)
                resourceUpdate.copyStrategy?.let { query.set(COPY_STRATEGY, it.name) }
                resourceUpdate.targetResourceType?.let { query.set(TARGET_RESOURCE_TYPE, it.name) }
                resourceUpdate.targetResourceId?.let { query.set(TARGET_RESOURCE_ID, it) }
                resourceUpdate.targetResourceName?.let { query.set(TARGET_RESOURCE_NAME, it) }
                resourceUpdate.targetResourceProperties?.let {
                    query.set(TARGET_RESOURCE_PROPERTIES, JsonUtil.toJson(it, formatted = false))
                }
                resourceUpdate.status?.let { query.set(STATUS, it.name) }
                resourceUpdate.errorMessage?.let { query.set(ERROR_MESSAGE, it) }
                resourceUpdate.targetNameExists?.let { query.set(TARGET_NAME_EXISTS, it) }
                resourceUpdate.targetIdExists?.let { query.set(TARGET_ID_EXISTS, it) }
                resourceUpdate.highRisk?.let { query.set(HIGH_RISK, it) }
                resourceUpdate.copyAction?.let { query.set(COPY_ACTION, it.name) }
                resourceUpdate.confirmed?.let { query.set(CONFIRMED, it) }
                query.set(UPDATE_TIME, LocalDateTime.now())
                    .where(PROJECT_ID.eq(resourceUpdate.projectId))
                    .and(TASK_ID.eq(resourceUpdate.taskId))
                    .and(RESOURCE_TYPE.eq(resourceUpdate.resourceType.name))
                    .and(RESOURCE_ID.eq(resourceUpdate.resourceId))
            }
        }
        return dslContext.batch(queries).execute().sum()
    }

    private fun convert(record: TPipelineCopyTaskResourceRecord): PipelineCopyTaskResource {
        return with(record) {
            PipelineCopyTaskResource(
                taskId = taskId,
                projectId = projectId,
                resourceType = PipelineDependentResourceType.valueOf(resourceType),
                resourceId = resourceId,
                resourceName = resourceName,
                resourceProperties = toProperties(resourceProperties),
                copyStrategy = copyStrategy?.let { PipelineCopyStrategy.valueOf(it) },
                targetProjectId = targetProjectId,
                targetResourceType = targetResourceType?.let {
                    PipelineDependentResourceType.valueOf(it)
                },
                targetResourceId = targetResourceId,
                targetResourceName = targetResourceName,
                targetResourceProp = toProperties(targetResourceProperties),
                status = PipelineCopyTaskResourceStatus.valueOf(status),
                errorMessage = errorMessage,
                highRisk = highRisk,
                targetNameExists = targetNameExists,
                targetIdExists = targetIdExists,
                copyAction = PipelineCopyAction.valueOf(copyAction),
                confirmed = confirmed,
                pipelineReferCount = pipelineReferCount,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }

    private fun buildConditions(
        projectId: String,
        taskId: String,
        resourceIds: Set<String>?,
        resourceType: PipelineDependentResourceType?,
        resourceName: String?,
        copyAction: PipelineCopyAction?,
        status: PipelineCopyTaskResourceStatus? = null
    ): List<Condition> {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(TASK_ID.eq(taskId))
            if (!resourceIds.isNullOrEmpty()) {
                conditions.add(RESOURCE_ID.`in`(resourceIds))
            }
            if (resourceType != null) {
                conditions.add(RESOURCE_TYPE.eq(resourceType.name))
            }
            if (!resourceName.isNullOrBlank()) {
                conditions.add(RESOURCE_NAME.like("%$resourceName%"))
            }
            if (copyAction != null) {
                conditions.add(COPY_ACTION.eq(copyAction.name))
            }
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            conditions
        }
    }

    private fun toProperties(value: String?): PipelineCopyResourceProp? {
        return value?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineCopyResourceProp::class.java)
        }
    }

}
