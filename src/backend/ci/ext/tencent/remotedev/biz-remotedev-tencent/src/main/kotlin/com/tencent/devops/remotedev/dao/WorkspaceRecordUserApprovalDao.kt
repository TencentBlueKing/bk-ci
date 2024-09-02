package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceRecordUserApproval
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WorkspaceRecordUserApprovalDao {
    fun addOrUpdateApproval(
        dslContext: DSLContext,
        projectId: String,
        user: String,
        workspaceName: String
    ) {
        with(TWorkspaceRecordUserApproval.T_WORKSPACE_RECORD_USER_APPROVAL) {
            dslContext.insertInto(
                this,
                WORKSPACE_NAME,
                USER,
                PROJECT_ID
            ).values(
                workspaceName,
                user,
                projectId
            ).onDuplicateKeyUpdate()
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun removeApproval(
        dslContext: DSLContext,
        workspaceName: String,
        user: String
    ) {
        with(TWorkspaceRecordUserApproval.T_WORKSPACE_RECORD_USER_APPROVAL) {
            dslContext.deleteFrom(this).where(WORKSPACE_NAME.eq(workspaceName)).and(USER.eq(user)).execute()
        }
    }

    fun checkApproval(
        dslContext: DSLContext,
        workspaceName: String,
        user: String
    ): Boolean {
        with(TWorkspaceRecordUserApproval.T_WORKSPACE_RECORD_USER_APPROVAL) {
            return dslContext.selectCount().from(this).where(WORKSPACE_NAME.eq(workspaceName)).and(USER.eq(user))
                .and(UPDATE_TIME.greaterThan(LocalDateTime.now().minusDays(7)))
                .fetchOne(0, Long::class.java)!! > 0
        }
    }
}