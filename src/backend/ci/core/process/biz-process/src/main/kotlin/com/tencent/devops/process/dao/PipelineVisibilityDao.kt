package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineVisibility
import com.tencent.devops.model.process.tables.records.TPipelineVisibilityRecord
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.pojo.PipelineVisibilityType
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineVisibilityDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ) {
        with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            val now = LocalDateTime.now()
            visibilityList.forEach {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    TYPE,
                    SCOPE_ID,
                    SCOPE_NAME,
                    CREATOR,
                    CREATE_TIME
                ).values(
                    projectId,
                    pipelineId,
                    it.type.name,
                    it.scopeId,
                    it.scopeName,
                    userId,
                    now
                )
                    .onDuplicateKeyUpdate()
                    .set(SCOPE_NAME, it.scopeName)
                    .set(CREATOR, userId)
                    .set(CREATE_TIME, now)
                    .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): Result<TPipelineVisibilityRecord> {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .orderBy(CREATE_TIME.desc(), TYPE)
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ) {
        with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            visibilityList.forEach {
                dslContext.deleteFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(TYPE.eq(it.type.name))
                    .and(SCOPE_ID.eq(it.scopeId))
                    .execute()
            }
        }
    }

    fun deleteByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun listVisiblePipelineIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: Set<String>,
        requestUserId: String,
        userDeptIds: Set<String>
    ): Set<String> {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            val deptCondition = TYPE.eq(PipelineVisibilityType.DEPT.name)
                .and(SCOPE_ID.`in`(userDeptIds))
            val userCondition = TYPE.eq(PipelineVisibilityType.USER.name)
                .and(SCOPE_ID.eq(requestUserId))
            dslContext.selectDistinct(PIPELINE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .and(DSL.or(deptCondition, userCondition))
                .fetch().map { it.value1() }.toSet()
        }
    }
}
