package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceSharedRecord
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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
                    )
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

    class TSharedRecordJooqMapper : RecordMapper<TWorkspaceSharedRecord, WorkspaceShared> {
        override fun map(record: TWorkspaceSharedRecord?): WorkspaceShared? {
            return record?.run {
                WorkspaceShared(
                    id = id,
                    workspaceName = workspaceName,
                    operator = operator,
                    sharedUser = sharedUser,
                    type = WorkspaceShared.AssignType.valueOf(assignType),
                    resourceId = resourceId
                )
            }
        }
    }

    companion object {
        val sharedMapper = TSharedRecordJooqMapper()
    }
}
