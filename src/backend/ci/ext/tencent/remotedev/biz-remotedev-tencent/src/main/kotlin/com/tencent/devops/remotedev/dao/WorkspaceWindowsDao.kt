package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.fetchCountFix
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TWindowsResourceType
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceWindowsRecord
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInfo
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class WorkspaceWindowsDao {
    fun batchFetchWorkspaceWindowsInfo(
        dslContext: DSLContext,
        workspaceNames: Set<String>
    ): Result<TWorkspaceWindowsRecord> {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.`in`(workspaceNames)).fetch()
        }
    }

    fun batchFetchWorkspaceWindowsInfoWithNodeIds(
        dslContext: DSLContext,
        nodeHashIds: Set<String>
    ): Result<Record3<String, String, String>> {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.select(WORKSPACE_NAME, HOST_IP, NODE_HASH_ID)
                .from(this)
                .where(NODE_HASH_ID.`in`(nodeHashIds))
                .fetch()
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

    fun updateDetailInfo(
        dslContext: DSLContext,
        launchId: Int?,
        regionId: Int?,
        vmName: String? = "",
        workspaceName: String
    ): Int {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.update(this)
                .set(CUR_LAUNCH_ID, launchId)
                .set(REGION_ID, regionId)
                .set(VM_NAME, vmName)
                .where(WORKSPACE_NAME.equal(workspaceName)).execute()
        }
    }

    fun updateNodeHashId(
        dslContext: DSLContext,
        nodeHashId: String?,
        workspaceName: String
    ): Int {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.update(this)
                .set(NODE_HASH_ID, nodeHashId)
                .where(WORKSPACE_NAME.equal(workspaceName)).execute()
        }
    }

    fun updateVmName(
        dslContext: DSLContext,
        vmName: String?,
        workspaceName: String
    ): Int {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.update(this)
                .set(VM_NAME, vmName)
                .where(WORKSPACE_NAME.equal(workspaceName)).limit(1).execute()
        }
    }

    fun countProjectIp(
        dslContext: DSLContext,
        projectId: String,
        ip: String
    ): Int {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.fetchCountFix(
                dslContext.select(IP)
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                    .and(IP.eq(ip))
            )
        }
    }

    fun countUserIp(
        dslContext: DSLContext,
        user: String,
        ip: String
    ): Int {
        return dslContext.fetchCountFix(
            dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP)
                .from(TWorkspace.T_WORKSPACE)
                .leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
                .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                .where(TWorkspace.T_WORKSPACE.CREATOR.eq(user))
                .and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                .and(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.like("%.$ip"))
                .and(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .and(TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name))
                .skipCheck()
                .unionAll(
                    dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP)
                        .from(TWorkspace.T_WORKSPACE)
                        .leftJoin(TWorkspaceShared.T_WORKSPACE_SHARED)
                        .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
                        .leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
                        .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                        .where(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.eq(user))
                        .and(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.like("%.$ip"))
                        .and(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name))
                        .and(TWorkspace.T_WORKSPACE.OWNER_TYPE.`in`(WorkspaceOwnerType.projectNames()))
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
            .from(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
            .leftJoin(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
            .on(
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.cast(Long::class.java)
                    .eq(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID)
            )
            .where(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME.`in`(workspaceNames))
            .and(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.eq(size))
            .fetchOne(0, Int::class.java)!!
    }

    fun bakWindowsConfig(
        dslContext: DSLContext,
        workspaceName: String,
        bakName: String
    ): Int {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            return dslContext.update(this)
                .set(WORKSPACE_NAME, bakName)
                .where(WORKSPACE_NAME.equal(workspaceName)).execute()
        }
    }

    fun fetchRecordByProjectIp(
        dslContext: DSLContext,
        projectId: String,
        ip: String?,
        workspaceName: String?
    ): WorkspaceRecordInfo? {
        if (ip.isNullOrBlank() && workspaceName.isNullOrBlank()) {
            return null
        }
        val dsl = dslContext.select(
            TWorkspace.T_WORKSPACE.PROJECT_ID,
            TWorkspace.T_WORKSPACE.NAME,
            TWorkspace.T_WORKSPACE.COFFEE_AI,
            TWorkspaceWindows.T_WORKSPACE_WINDOWS.ENABLE_RECORD_USER,
            TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP
        ).from(TWorkspace.T_WORKSPACE)
            .leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
            .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
            .where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId))
            .and(
                TWorkspace.T_WORKSPACE.STATUS.notIn(
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            )
        val res = if (!workspaceName.isNullOrBlank()) {
            dsl.and(TWorkspace.T_WORKSPACE.NAME.eq(ip)).fetchAny() ?: return null
        } else {
            dsl.and(TWorkspace.T_WORKSPACE.IP.eq(ip)).fetchAny() ?: return null
        }
        return WorkspaceRecordInfo(
            projectId = res.value1(),
            workspaceName = res.value2(),
            coffeeAIEnable = res.value3()?.let { it != 0.toByte() } ?: false,
            enableUser = res.value4(),
            hostIp = res.value5()
        )
    }

    fun updateRecord(
        dslContext: DSLContext,
        workspaceName: String,
        enableUser: String?
    ) {
        with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
            dslContext.update(this).set(ENABLE_RECORD_USER, enableUser)
                .where(WORKSPACE_NAME.eq(workspaceName)).execute()
        }
    }
}
