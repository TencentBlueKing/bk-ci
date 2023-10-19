package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceWindowsRecord
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
        hostIp: String? = ""
    ): Int {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.update(this)
                .set(RESOURCE_ID, resourceId ?: "")
                .set(HOST_IP, hostIp ?: "")
                .where(WORKSPACE_NAME.equal(workspaceName)).execute()
        }
    }
}
