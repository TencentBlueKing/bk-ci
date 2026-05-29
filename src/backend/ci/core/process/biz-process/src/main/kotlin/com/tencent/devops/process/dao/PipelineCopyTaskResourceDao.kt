package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_COPY_TASK_RESOURCE
import com.tencent.devops.model.process.tables.records.TPipelineCopyTaskResourceRecord
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceProperties
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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
                    CONFIRMED
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
                    resource.targetResourceProperties?.let { JsonUtil.toJson(it, formatted = false) },
                    resource.status.name,
                    resource.errorMessage,
                    resource.highRisk,
                    resource.targetNameExists,
                    resource.targetIdExists,
                    resource.copyAction.name,
                    resource.confirmed
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
        copyAction: PipelineCopyAction? = null
    ): List<PipelineCopyTaskResourceInfo> {
        if (resourceIds != null && resourceIds.isEmpty()) {
            return emptyList()
        }
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.selectFrom(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        taskId = taskId,
                        resourceIds = resourceIds,
                        resourceType = resourceType,
                        resourceName = resourceName,
                        copyAction = copyAction
                    )
                )
                .orderBy(RESOURCE_TYPE.asc(), RESOURCE_NAME.asc(), RESOURCE_ID.asc())
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

    fun updateDraft(
        dslContext: DSLContext,
        update: PipelineCopyTaskResourceUpdate
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE) {
            dslContext.update(this)
                .set(COPY_STRATEGY, update.copyStrategy?.name)
                .set(
                    TARGET_RESOURCE_PROPERTIES,
                    update.targetResourceProperties?.let { JsonUtil.toJson(it, formatted = false) }
                )
                .set(COPY_ACTION, update.copyAction?.name)
                .set(STATUS, update.status?.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .and(RESOURCE_TYPE.eq(update.resourceType.name))
                .and(RESOURCE_ID.eq(update.resourceId))
                .execute()
        }
    }

    private fun convert(record: TPipelineCopyTaskResourceRecord): PipelineCopyTaskResourceInfo {
        return with(record) {
            PipelineCopyTaskResourceInfo(
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
                targetResourceProperties = toProperties(targetResourceProperties),
                status = PipelineCopyTaskResourceStatus.valueOf(status),
                errorMessage = errorMessage,
                highRisk = highRisk,
                targetNameExists = targetNameExists,
                targetIdExists = targetIdExists,
                copyAction = PipelineCopyAction.valueOf(copyAction),
                confirmed = confirmed,
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
        copyAction: PipelineCopyAction?
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
            conditions
        }
    }

    private fun toProperties(value: String?): PipelineCopyResourceProperties? {
        return value?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineCopyResourceProperties::class.java)
        }
    }

}
