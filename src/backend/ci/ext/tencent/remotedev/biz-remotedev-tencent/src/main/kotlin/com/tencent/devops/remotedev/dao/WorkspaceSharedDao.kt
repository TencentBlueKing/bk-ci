package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceSharedRecord
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class WorkspaceSharedDao {

    fun batchCreate(
        dslContext: DSLContext,
        workspaceName: String,
        operator: String,
        assigns: List<ProjectWorkspaceAssign>,
        resourceId: String
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.batch(
                assigns.map {
                    DSL.insertInto(
                        this,
                        WORKSPACE_NAME,
                        OPERATOR,
                        SHARED_USER,
                        ASSIGN_TYPE,
                        RESOURCE_ID,
                        EXPIRATION
                    ).values(
                        workspaceName,
                        operator,
                        it.userId,
                        it.type.name,
                        resourceId,
                        it.expiration
                    ).onDuplicateKeyUpdate()
                        .set(RESOURCE_ID, resourceId)
                        .set(EXPIRATION, it.expiration)
                }
            ).execute()
        }
    }

    fun existWorkspaceSharedInfo(
        workspaceName: String,
        sharedUser: String,
        dslContext: DSLContext
    ): Boolean {
        return with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.selectCount().from(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(SHARED_USER.eq(sharedUser))
                .and(ASSIGN_TYPE.eq(WorkspaceShared.AssignType.VIEWER.name))
                .fetchOne(0, Int::class.java)!! > 0
        }
    }

    fun fetchWorkspaceSharedInfo(
        dslContext: DSLContext,
        workspaceName: String,
        sharedUsers: List<String>? = null,
        assignType: WorkspaceShared.AssignType? = null
    ): List<WorkspaceShared> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .let { if (!sharedUsers.isNullOrEmpty()) it.and(SHARED_USER.`in`(sharedUsers)) else it }
                .let { if (assignType != null) it.and(ASSIGN_TYPE.eq(assignType.name)) else it }
                .fetch(sharedMapper)
        }
    }

    fun batchFetchWorkspaceSharedInfo(
        dslContext: DSLContext,
        workspaceNames: Set<String>
    ): List<WorkspaceShared> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.`in`(workspaceNames)).fetch(sharedMapper)
        }
    }

    fun fetchSharedWorkspaceById(
        id: Long,
        dslContext: DSLContext
    ): WorkspaceShared? {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .limit(1)
                .fetchAny(sharedMapper)
        }
    }

    fun fetchSharedWorkspaceByUser(
        dslContext: DSLContext,
        workspaceName: String,
        sharedUser: String
    ): WorkspaceShared? {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(SHARED_USER.eq(sharedUser))
                .fetchAny(sharedMapper)
        }
    }

    fun batchDelete(
        dslContext: DSLContext,
        workspaceName: String,
        sharedUsers: List<String>,
        assignType: WorkspaceShared.AssignType
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.delete(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(SHARED_USER.`in`(sharedUsers))
                .and(ASSIGN_TYPE.eq(assignType.name))
                .execute()
        }
    }

    fun fetchExpireShare(
        dslContext: DSLContext
    ): List<TWorkspaceSharedRecord> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this)
                .where(EXPIRATION.isNotNull)
                .and(EXPIRATION.lessThan(LocalDateTime.now()))
                .fetch()
        }
    }

    fun checkAlreadyExpireShare(
        dslContext: DSLContext,
        workspaceName: String,
        operator: String,
        sharedUser: String,
        assignType: WorkspaceShared.AssignType
    ): Boolean {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(OPERATOR.eq(operator))
                .and(SHARED_USER.eq(sharedUser))
                .and(ASSIGN_TYPE.eq(assignType.name))
                .and(EXPIRATION.isNotNull)
                .and(EXPIRATION.greaterThan(LocalDateTime.now()))
                .fetchAny() != null
        }
    }

    fun fetchWorkspaceOwner(
        dslContext: DSLContext,
        workspaceNames: Set<String>
    ): Map<String, String> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.`in`(workspaceNames))
                .and(ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name))
                .fetch().associateBy({ it.workspaceName }, { it.sharedUser })
        }
    }

    /**
     * 模糊匹配workspaceName 拿分享信息
     */
    fun fetchSharedWorkspace(
        dslContext: DSLContext,
        workspaceName: String? = null
    ): Result<out Record>? {
        val t1 = TWorkspace.T_WORKSPACE.`as`("t1")
        val t2 = TWorkspaceShared.T_WORKSPACE_SHARED.`as`("t2")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.STATUS.ne(WorkspaceStatus.DELETED.ordinal))
        conditions.add(t2.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.VIEWER.name))
        if (!workspaceName.isNullOrBlank()) {
            conditions.add(t2.WORKSPACE_NAME.like("%$workspaceName%"))
        }
        return dslContext.select(t2.ID, t2.WORKSPACE_NAME, t2.OPERATOR, t2.SHARED_USER, t2.ASSIGN_TYPE, t2.RESOURCE_ID)
            .from(t1).innerJoin(t2).on(t1.NAME.eq(t2.WORKSPACE_NAME))
            .where(conditions)
            .fetch()
    }

    fun bakWorkspaceShareInfo(
        dslContext: DSLContext,
        workspaceName: String,
        bakName: String
    ): Int {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.update(this)
                .set(WORKSPACE_NAME, bakName)
                .where(WORKSPACE_NAME.equal(workspaceName)).execute()
        }
    }

    fun deleteOwner(
        dslContext: DSLContext,
        workspaceName: String
    ): Int {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.deleteFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name))
                .limit(1)
                .execute()
        }
    }

    class TSharedRecordJooqMapper : RecordMapper<TWorkspaceSharedRecord, WorkspaceShared> {
        override fun map(record: TWorkspaceSharedRecord?): WorkspaceShared? {
            return record?.run {
                WorkspaceShared(
                    id = id,
                    workspaceName = workspaceName,
                    operator = operator,
                    sharedUser = sharedUser,
                    type = WorkspaceShared.AssignType.parse(assignType),
                    resourceId = resourceId
                )
            }
        }
    }

    companion object {
        val sharedMapper = TSharedRecordJooqMapper()
    }
}
