package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TWindowsResourceType
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceWindowsRecord
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class WorkspaceWindowsDao {

    fun opCreate(
        dslContext: DSLContext,
        workspaceName: String,
        winConfigId: Int?,
        resourceId: String = ""
    ): Int {
        return with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            dslContext.insertInto(
                this,
                WORKSPACE_NAME,
                WIN_CONFIG_ID
            ).values(
                workspaceName,
                winConfigId
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun batchFetchWorkspaceWindowsInfo(
        dslContext: DSLContext,
        workspaceNames: List<String>
    ): Result<TWorkspaceWindowsRecord> {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.`in`(workspaceNames)).fetch()
        }
    }

    fun fetchAnyWorkspaceWindowsInfo(
        dslContext: DSLContext,
        workspaceName: String
    ): TWorkspaceWindowsRecord? {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.equal(workspaceName)).fetchAny()
        }
    }

    fun updateWindowsResourceId(
        dslContext: DSLContext,
        workspaceName: String,
        resourceId: String? = "",
        hostIp: String? = "",
        macAddress: String? = ""
    ): Int {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.update(this)
                .set(RESOURCE_ID, resourceId ?: "")
                .set(HOST_IP, hostIp ?: "")
                .set(MAC_ADDRESS, macAddress ?: "")
                .where(WORKSPACE_NAME.equal(workspaceName)).execute()
        }
    }

    fun countProjectIp(
        dslContext: DSLContext,
        projectId: String,
        ip: String
    ): Int {
        return dslContext.fetchCount(
            dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP)
                .from(TWorkspace.T_WORKSPACE, TWorkspaceWindows.T_WORKSPACE_WINDOWS)
                .where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId))
                .and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                .and(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .and(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.like("%.$ip"))
        )
    }

    fun countUserIp(
        dslContext: DSLContext,
        user: String,
        ip: String
    ): Int {
        return dslContext.fetchCount(
            dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP)
                .from(TWorkspace.T_WORKSPACE, TWorkspaceWindows.T_WORKSPACE_WINDOWS)
                .where(TWorkspace.T_WORKSPACE.CREATOR.eq(user))
                .and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                .and(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.like("%.$ip"))
                .and(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .and(TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name))
                .skipCheck()
                .unionAll(
                    dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP)
                        .from(
                            TWorkspace.T_WORKSPACE, TWorkspaceShared.T_WORKSPACE_SHARED,
                            TWorkspaceWindows.T_WORKSPACE_WINDOWS
                        )
                        .where(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.eq(user))
                        .and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                        .and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
                        .and(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.like("%.$ip"))
                        .and(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name))
                        .and(TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PROJECT.name))
                        .skipCheck()
                )
        )
    }

    /**
     * 查询用户使用了多少台指定机型的机器
     */
    fun fetchUsedSizeCount(
        dslContext: DSLContext,
        workspaceNames: Set<String>,
        size: String
    ): Int {
        return dslContext.selectCount()
            .from(TWorkspaceWindows.T_WORKSPACE_WINDOWS, TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
            .where(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME.`in`(workspaceNames))
            .and(
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.cast(Long::class.java)
                    .eq(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID)
            )
            .and(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.eq(size))
            .fetchOne(0, Int::class.java)!!
    }
}
