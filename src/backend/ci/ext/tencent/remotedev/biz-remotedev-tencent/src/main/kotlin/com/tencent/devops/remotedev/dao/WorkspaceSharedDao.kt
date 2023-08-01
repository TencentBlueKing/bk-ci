package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class WorkspaceSharedDao {

    // 新增工作空间共享记录
    fun createWorkspaceSharedInfo(
        userId: String,
        workspaceShared: WorkspaceShared,
        dslContext: DSLContext
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.insertInto(
                this,
                WORKSPACE_NAME,
                OPERATOR,
                SHARED_USER,
                ASSIGN_TYPE
            )
                .values(
                    workspaceShared.workspaceName,
                    userId,
                    workspaceShared.sharedUser,
                    workspaceShared.type.name
                ).execute()
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        workspaceName: String,
        operator: String,
        assigns: List<ProjectWorkspaceAssign>
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.batch(
                assigns.map {
                    DSL.insertInto(
                        this,
                        WORKSPACE_NAME,
                        OPERATOR,
                        SHARED_USER,
                        ASSIGN_TYPE
                    ).values(
                        workspaceName,
                        operator,
                        it.userId,
                        it.type.name
                    )
                }
            ).execute()
        }
    }

    fun existWorkspaceSharedInfo(
        workspaceShared: WorkspaceShared,
        dslContext: DSLContext
    ): Boolean {
        return with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.selectCount().from(this)
                .where(WORKSPACE_NAME.eq(workspaceShared.workspaceName))
                .and(SHARED_USER.eq(workspaceShared.sharedUser))
                .fetchOne(0, Int::class.java)!! > 0
        }
    }

    fun fetchWorkspaceSharedInfo(
        dslContext: DSLContext,
        workspaceName: String
    ): List<WorkspaceShared> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.eq(workspaceName)).fetch().map {
                WorkspaceShared(
                    id = it.id,
                    workspaceName = it.workspaceName,
                    operator = it.operator,
                    sharedUser = it.sharedUser,
                    type = WorkspaceShared.AssignType.valueOf(it.assignType)
                )
            }
        }
    }

    // 删除工作空间共享记录
    fun deleteWorkspaceSharedInfo(
        workspaceName: String,
        sharedUser: String,
        dslContext: DSLContext
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.delete(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(SHARED_USER.equals(sharedUser))
                .limit(1)
        }
    }
}
