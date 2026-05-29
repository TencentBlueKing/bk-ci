package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_COPY_TASK_RESOURCE_REL
import com.tencent.devops.model.process.tables.records.TPipelineCopyTaskResourceRelRecord
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyPipelineInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceRelInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineCopyTaskResourceRelDao {

    fun batchCreate(
        dslContext: DSLContext,
        relations: List<PipelineCopyTaskResourceRelInfo>
    ) {
        if (relations.isEmpty()) {
            return
        }
        val queries = with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
            relations.map { relation ->
                dslContext.insertInto(
                    this,
                    TASK_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    RESOURCE_TYPE,
                    RESOURCE_ID,
                    RESOURCE_NAME
                ).values(
                    relation.taskId,
                    relation.projectId,
                    relation.pipelineId,
                    relation.pipelineName,
                    relation.resourceType.name,
                    relation.resourceId,
                    relation.resourceName
                ).onDuplicateKeyIgnore()
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
        resourceType: PipelineDependentResourceType? = null,
        resourceName: String? = null,
        pipelineName: String? = null
    ): List<PipelineCopyTaskResourceRelInfo> {
        if ((pipelineIds != null && pipelineIds.isEmpty()) || (resourceIds != null && resourceIds.isEmpty())) {
            return emptyList()
        }
        return with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
            dslContext.selectFrom(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineIds = pipelineIds,
                        resourceIds = resourceIds,
                        resourceType = resourceType,
                        resourceName = resourceName,
                        pipelineName = pipelineName
                    )
                )
                .orderBy(PIPELINE_NAME.asc(), PIPELINE_ID.asc())
                .fetch()
                .map(::convert)
        }
    }

    fun listResourcePipelines(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        pipelineName: String?
    ): List<PipelineCopyPipelineInfo> {
        return with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
            dslContext.select(PIPELINE_ID, PIPELINE_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(RESOURCE_TYPE.eq(resourceType.name))
                .and(RESOURCE_ID.eq(resourceId))
                .let {
                    if (!pipelineName.isNullOrBlank()) {
                        it.and(PIPELINE_NAME.like("%$pipelineName%"))
                    } else {
                        it
                    }
                }
                .groupBy(PIPELINE_ID, PIPELINE_NAME)
                .orderBy(PIPELINE_NAME.asc(), PIPELINE_ID.asc())
                .fetch {
                    PipelineCopyPipelineInfo(
                        pipelineId = it[PIPELINE_ID],
                        pipelineName = it[PIPELINE_NAME]
                    )
                }
        }
    }

    fun deleteByTaskId(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): Int {
        return with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
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
        if (pipelineIds.isEmpty()) {
            return 0
        }
        return with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
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
        return with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(RESOURCE_TYPE.ne(PipelineDependentResourceType.PIPELINE.name))
                .execute()
        }
    }

    private fun convert(record: TPipelineCopyTaskResourceRelRecord): PipelineCopyTaskResourceRelInfo {
        return with(record) {
            PipelineCopyTaskResourceRelInfo(
                taskId = taskId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                resourceType = PipelineDependentResourceType.valueOf(resourceType),
                resourceId = resourceId,
                resourceName = resourceName,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }

    private fun buildConditions(
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>?,
        resourceIds: Set<String>?,
        resourceType: PipelineDependentResourceType?,
        resourceName: String?,
        pipelineName: String?
    ): List<Condition> {
        return with(T_PIPELINE_COPY_TASK_RESOURCE_REL) {
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
            if (!resourceName.isNullOrBlank()) {
                conditions.add(RESOURCE_NAME.like("%$resourceName%"))
            }
            if (!pipelineName.isNullOrBlank()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            conditions
        }
    }
}
