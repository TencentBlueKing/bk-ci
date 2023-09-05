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
                        RESOURCE_ID
                    ).values(
                        workspaceName,
                        operator,
                        it.userId,
                        it.type.name,
                        resourceId
                    )
                }
            ).execute()
        }
    }

    fun updateResourceId(
        dslContext: DSLContext,
        workspaceName: String,
        sharedUser: List<String>,
        resourceId: String? = ""
    ): Int {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.update(this)
                .set(RESOURCE_ID, resourceId ?: "")
                .where(WORKSPACE_NAME.equal(workspaceName).and(SHARED_USER.`in`(sharedUser))).execute()
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
                .fetch().map {
                    WorkspaceShared(
                        id = it.id,
                        workspaceName = it.workspaceName,
                        operator = it.operator,
                        sharedUser = it.sharedUser,
                        type = WorkspaceShared.AssignType.valueOf(it.assignType),
                        resourceId = it.resourceId
                    )
                }
        }
    }

    fun batchFetchWorkspaceSharedInfo(
        dslContext: DSLContext,
        workspaceNames: List<String>
    ): List<WorkspaceShared> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.`in`(workspaceNames)).fetch().map {
                WorkspaceShared(
                    id = it.id,
                    workspaceName = it.workspaceName,
                    operator = it.operator,
                    sharedUser = it.sharedUser,
                    type = WorkspaceShared.AssignType.valueOf(it.assignType),
                    resourceId = it.resourceId
                )
            }
        }
    }

    fun batchSelectAssignType(
        dslContext: DSLContext,
        userId: String,
        workspaceNames: List<String>
    ): Map<String, WorkspaceShared.AssignType> {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.select(WORKSPACE_NAME, ASSIGN_TYPE).from(this).where(SHARED_USER.eq(userId))
                .and(WORKSPACE_NAME.`in`(workspaceNames)).fetch()
                .associateBy({ it.value1() }, { WorkspaceShared.AssignType.valueOf(it.value2()) })
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
                .execute()
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
}
