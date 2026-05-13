package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.tables.TPipelineVisibility
import com.tencent.devops.model.process.tables.records.TPipelineVisibilityRecord
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.pojo.PipelineVisibilityType
import org.jooq.Condition
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
        authUser: String,
        visibilityList: List<PipelineVisibility>
    ) {
        with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            val now = LocalDateTime.now()
            visibilityList.forEach {
                val userDepartments = it.userDepartments?.let { u -> JsonUtil.toJson(u, false) }
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    TYPE,
                    SCOPE_ID,
                    SCOPE_NAME,
                    FULL_NAME,
                    USER_DEPARTMENTS,
                    AUTH_USER,
                    CREATOR,
                    CREATE_TIME,
                    UPDATER,
                    UPDATE_TIME
                ).values(
                    projectId,
                    pipelineId,
                    it.type.name,
                    it.scopeId,
                    it.scopeName,
                    it.fullName,
                    userDepartments,
                    authUser,
                    userId,
                    now,
                    userId,
                    now
                )
                    .onDuplicateKeyUpdate()
                    .set(SCOPE_NAME, it.scopeName)
                    .set(FULL_NAME, it.fullName)
                    .set(USER_DEPARTMENTS, userDepartments)
                    .set(AUTH_USER, authUser)
                    .set(UPDATER, userId)
                    .set(UPDATE_TIME, now)
                    .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        keyword: String? = null,
        limit: Int,
        offset: Int
    ): Result<TPipelineVisibilityRecord> {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.selectFrom(this)
                .where(buildListConditions(projectId, pipelineId, keyword))
                .orderBy(CREATE_TIME.desc(), TYPE)
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        keyword: String? = null
    ): Int {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.selectCount()
                .from(this)
                .where(buildListConditions(projectId, pipelineId, keyword))
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

    fun deleteByScopeIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        scopeIds: List<String>
    ) {
        if (scopeIds.isEmpty()) {
            return
        }
        with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(SCOPE_ID.`in`(scopeIds))
                .execute()
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

    fun updateAuthUser(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        authUser: String
    ) {
        with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            dslContext.update(this)
                .set(AUTH_USER, authUser)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun countVisibilityPipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        userDeptIds: Set<String>
    ): Int {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            val userCondition = TYPE.eq(PipelineVisibilityType.USER.name)
                .and(SCOPE_ID.eq(userId))
            val orgCondition = TYPE.eq(PipelineVisibilityType.ORG.name)
                .and(SCOPE_ID.`in`(userDeptIds))
            dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(DSL.or(userCondition, orgCondition))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun listVisiblePipelineIds(
        dslContext: DSLContext,
        projectId: String,
        authUser: String,
        requestUserId: String,
        userDeptIds: Set<String>,
        pipelineIds: Set<String>? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<String> {
        return with(TPipelineVisibility.T_PIPELINE_VISIBILITY) {
            val conditions = buildVisibilityConditions(
                projectId = projectId,
                authUser = authUser,
                requestUserId = requestUserId,
                userDeptIds = userDeptIds,
                pipelineIds = pipelineIds
            )
            val query = dslContext.selectDistinct(PIPELINE_ID)
                .from(this)
                .where(conditions)
            if (limit != null && offset != null) {
                query.limit(limit).offset(offset)
            }
            query.fetch().map { it.value1() }
        }
    }

    private fun TPipelineVisibility.buildVisibilityConditions(
        projectId: String,
        authUser: String,
        requestUserId: String,
        userDeptIds: Set<String>,
        pipelineIds: Set<String>? = null
    ): List<Condition> {
        val orgCondition = TYPE.eq(PipelineVisibilityType.ORG.name)
            .and(SCOPE_ID.`in`(userDeptIds))
        val userCondition = TYPE.eq(PipelineVisibilityType.USER.name)
            .and(SCOPE_ID.eq(requestUserId))
        val conditions = mutableListOf(
            PROJECT_ID.eq(projectId),
            AUTH_USER.eq(authUser),
            DSL.or(orgCondition, userCondition)
        )
        if (!pipelineIds.isNullOrEmpty()) {
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
        }
        return conditions
    }

    private fun TPipelineVisibility.buildListConditions(
        projectId: String,
        pipelineId: String,
        keyword: String?
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(PROJECT_ID.eq(projectId))
        conditions.add(PIPELINE_ID.eq(pipelineId))
        if (!keyword.isNullOrBlank()) {
            conditions.add(SCOPE_NAME.like("%$keyword%"))
        }
        return conditions
    }
}
