/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.model.remotedev.tables.TRemoteDevSettings
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class WorkspaceDao {

    fun createWorkspace(
        workspace: Workspace,
        workspaceStatus: WorkspaceStatus,
        bgName: String?,
        deptName: String?,
        centerName: String?,
        groupName: String?,
        dslContext: DSLContext
    ): Long {
        return with(TWorkspace.T_WORKSPACE) {
            dslContext.insertInto(
                /* into = */ this,
                /* ...fields = */ PROJECT_ID,
                NAME,
                TEMPLATE_ID,
                URL,
                BRANCH,
                YAML_PATH,
                IMAGE_PATH,
                WORK_PATH,
                WORKSPACE_FOLDER,
                HOST_NAME,
                GPU,
                CPU,
                MEMORY,
                DISK,
                STATUS,
                LAST_STATUS_UPDATE_TIME,
                YAML,
                DOCKERFILE,
                CREATOR,
                CREATOR_BG_NAME,
                CREATOR_DEPT_NAME,
                CREATOR_CENTER_NAME,
                CREATOR_GROUP_NAME,
                WORKSPACE_MOUNT_TYPE,
                SYSTEM_TYPE,
                OWNER_TYPE,
                WIN_CONFIG_ID
            )
                .values(
                    workspace.projectId,
                    workspace.workspaceName,
                    workspace.wsTemplateId,
                    workspace.repositoryUrl,
                    workspace.branch,
                    workspace.devFilePath,
                    "",
                    workspace.workPath,
                    workspace.workspaceFolder,
                    workspace.hostName,
                    workspace.gpu,
                    workspace.cpu,
                    workspace.memory,
                    workspace.disk,
                    workspaceStatus.ordinal,
                    LocalDateTime.now(),
                    workspace.yaml,
                    "",
                    workspace.createUserId,
                    bgName ?: "",
                    deptName ?: "",
                    centerName ?: "",
                    groupName ?: "",
                    workspace.workspaceMountType.name,
                    workspace.workspaceSystemType.name,
                    workspace.ownerType.name,
                    workspace.winConfigId
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun limitFetchWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        userId: String? = null,
        workspaceName: String? = null
    ): Result<TWorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId, workspaceName)
            val query = dslContext.selectFrom(this)

            if (condition.isNotEmpty()) {
                query.where(condition)
            }
            return query.orderBy(CREATE_TIME.desc(), ID.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch()
        }
    }

    /**
     * 获得用户所拥有工作空间列表（计数）
     */
    fun countUserWorkspace(
        dslContext: DSLContext,
        creator: String,
        unionShared: Boolean = true,
        ownerType: WorkspaceOwnerType? = null,
        status: Set<WorkspaceStatus>? = null,
        systemType: WorkspaceSystemType? = null
    ): Long {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectCount().from(this)
                .where(CREATOR.eq(creator))
                .let {
                    if (status.isNullOrEmpty()) {
                        it.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                    } else {
                        it.and(STATUS.`in`(status.map { s -> s.ordinal }))
                    }
                }
                .let { if (systemType != null) it.and(SYSTEM_TYPE.eq(systemType.name)) else it }
                .let { if (ownerType != null) it.and(OWNER_TYPE.eq(ownerType.name)) else it }
                .let {
                    if (unionShared) it.unionAll(
                        unionSelect(shared, creator, status, systemType, ownerType)
                    ) else it
                }
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun TWorkspace.unionSelect(
        shared: TWorkspaceShared,
        creator: String,
        status: Set<WorkspaceStatus>?,
        systemType: WorkspaceSystemType?,
        ownerType: WorkspaceOwnerType?
    ) = DSL.selectCount().from(this).where(
        NAME.`in`(
            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                shared.SHARED_USER.eq(
                    creator
                )
            )
        )
    ).let { i ->
        if (status.isNullOrEmpty()) {
            i.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
        } else {
            i.and(STATUS.`in`(status.map { s -> s.ordinal }))
        }
    }.let { i -> if (systemType != null) i.and(SYSTEM_TYPE.eq(systemType.name)) else i }
        .let { i -> if (ownerType != null) i.and(OWNER_TYPE.eq(ownerType.name)) else i }

    /**
     * 获得用户所拥有工作空间列表
     */
    fun limitFetchUserWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        creator: String,
        ownerType: WorkspaceOwnerType? = null
    ): Result<TWorkspaceRecord>? {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(CREATOR.eq(creator))
                .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .let { i -> if (ownerType != null) i.and(OWNER_TYPE.eq(ownerType.name)) else i }
                .unionAll(
                    DSL.selectFrom(this).where(
                        NAME.`in`(
                            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                                shared.SHARED_USER.eq(
                                    creator
                                )
                            )
                        )
                    ).and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                        .let { i -> if (ownerType != null) i.and(OWNER_TYPE.eq(ownerType.name)) else i }
                ).orderBy(CREATE_TIME.desc(), ID.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch()
        }
    }

    /**
     * 获得拥有该工作空间的用户
     */
    fun fetchWorkspaceUser(
        dslContext: DSLContext,
        workspaceName: String
    ): List<String> {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.select(CREATOR).from(this)
                .where(NAME.eq(workspaceName)).unionAll(
                    DSL.select(shared.SHARED_USER).from(shared).where(
                        shared.WORKSPACE_NAME.eq(workspaceName)
                    )
                ).fetch(0, String::class.java)
        }
    }

    fun fetchCreators(
        dslContext: DSLContext,
        status: WorkspaceStatus,
        systemType: WorkspaceSystemType = WorkspaceSystemType.WINDOWS_GPU
    ): List<Record2<String, String>> {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.select(CREATOR, NAME).from(this)
                .where(STATUS.eq(status.ordinal))
                .and(SYSTEM_TYPE.eq(systemType.name))
                .fetch()
        }
    }

    fun fetchAnyWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
        workspaceName: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null
    ): TWorkspaceRecord? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(
                userId = userId,
                workspaceName = workspaceName,
                status = status,
                mountType = mountType
            )

            if (condition.isEmpty()) {
                return null
            }

            return dslContext.selectFrom(this)
                .where(condition)
                .fetchAny()
        }
    }

    fun fetchWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null
    ): Result<TWorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(
                userId = userId,
                status = status,
                mountType = mountType
            )

            if (condition.isEmpty()) {
                return null
            }

            return dslContext.selectFrom(this)
                .where(condition)
                .fetch()
        }
    }

    /**
     * 获取没有使用时长的工作空间
     */
    fun fetchNotUsageTimeWinWorkspace(
        dslContext: DSLContext,
        status: WorkspaceStatus,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): Result<TWorkspaceRecord>? {
        val setting = TRemoteDevSettings.T_REMOTE_DEV_SETTINGS
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this).where(
                CREATOR.`in`(
                    DSL.select(setting.USER_ID).from(setting).where(setting.WIN_USAGE_REMAINING_TIME.le(0))
                )
            ).and(STATUS.eq(status.ordinal)).and(WORKSPACE_MOUNT_TYPE.eq(WorkspaceMountType.START.name))
                .and(OWNER_TYPE.eq(ownerType.name))
                .fetch()
        }
    }

    fun fetchSharedWorkspace(
        dslContext: DSLContext,
        workspaceName: String? = null
    ): Result<out Record>? {
        val t1 = TWorkspace.T_WORKSPACE.`as`("t1")
        val t2 = TWorkspaceShared.T_WORKSPACE_SHARED.`as`("t2")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.STATUS.ne(WorkspaceStatus.DELETED.ordinal))
        if (!workspaceName.isNullOrBlank()) {
            conditions.add(t2.WORKSPACE_NAME.like("%$workspaceName%"))
        }
        return dslContext.select(t2.ID, t2.WORKSPACE_NAME, t2.OPERATOR, t2.SHARED_USER, t2.ASSIGN_TYPE)
            .from(t1).leftJoin(t2).on(t1.NAME.eq(t2.WORKSPACE_NAME))
            .where(conditions)
            .fetch()
    }

    fun deleteSharedWorkspace(
        id: Long,
        dslContext: DSLContext
    ): Int {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            return dslContext.delete(this)
                .where(ID.eq(id))
                .limit(1)
                .execute()
        }
    }

    private fun mixCondition(
        userId: String? = null,
        workspaceName: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null
    ): List<Condition> {
        val condition = mutableListOf<Condition>()
        with(TWorkspace.T_WORKSPACE) {
            if (!userId.isNullOrBlank()) {
                condition.add(CREATOR.eq(userId))
            }
            if (!workspaceName.isNullOrBlank()) {
                condition.add(NAME.eq(workspaceName))
            }
            if (status != null) {
                condition.add(STATUS.eq(status.ordinal))
            }
            if (mountType != null) {
                condition.add(WORKSPACE_MOUNT_TYPE.eq(mountType.name))
            }
        }
        return condition
    }

    fun updateWorkspaceStatus(
        dslContext: DSLContext,
        workspaceName: String,
        status: WorkspaceStatus,
        hostName: String? = ""
    ) {
        with(TWorkspace.T_WORKSPACE) {
            if (hostName.isNullOrBlank()) {
                dslContext.update(this)
                    .set(STATUS, status.ordinal)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
                    .where(NAME.eq(workspaceName))
                    .execute()
            } else {
                dslContext.update(this)
                    .set(STATUS, status.ordinal)
                    .set(HOST_NAME, hostName)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
                    .where(NAME.eq(workspaceName))
                    .execute()
            }
        }
    }

    fun updateWorkspaceDisplayName(
        dslContext: DSLContext,
        workspaceName: String,
        displayName: String? = ""
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(DISPLAY_NAME, displayName)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(NAME.eq(workspaceName))
                .execute()
        }
    }

    fun updateWorkspaceCreatorInfo(
        dslContext: DSLContext,
        workspaceName: String,
        creator: String,
        bgName: String,
        deptName: String,
        centerName: String,
        groupName: String
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(CREATOR_BG_NAME, bgName)
                .set(CREATOR_DEPT_NAME, deptName)
                .set(CREATOR_CENTER_NAME, centerName)
                .set(CREATOR_GROUP_NAME, groupName)
                .where(NAME.eq(workspaceName).and(CREATOR.eq(creator)))
                .execute()
        }
    }

    fun updateWorkspaceUsageTime(
        workspaceName: String,
        usageTime: Int,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(USAGE_TIME, USAGE_TIME + usageTime)
                .where(NAME.eq(workspaceName))
                .execute()
        }
    }

    fun updateWorkspaceSleepingTime(
        workspaceName: String,
        sleepTime: Int,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(SLEEPING_TIME, SLEEPING_TIME + sleepTime)
                .where(NAME.eq(workspaceName))
                .execute()
        }
    }

    fun deleteWorkspace(
        workspaceName: String,
        dslContext: DSLContext
    ): Int {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.delete(this)
                .where(NAME.eq(workspaceName))
                .limit(1)
                .execute()
        }
    }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun getTimeOutInactivityWorkspace(
        dslContext: DSLContext,
        timeOutDays: Int,
        systemType: WorkspaceSystemType?,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): Result<TWorkspaceRecord> {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mutableListOf<Condition>()
            condition.add(
                timestampDiff(DatePart.DAY, LAST_STATUS_UPDATE_TIME.cast(java.sql.Timestamp::class.java))
                    .greaterOrEqual(timeOutDays)
            )

            condition.add(STATUS.eq(WorkspaceStatus.SLEEP.ordinal))
            condition.add(OWNER_TYPE.eq(ownerType.name))

            if (systemType != null) {
                condition.add(SYSTEM_TYPE.eq(systemType.name))
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .limit(1000)
                .fetch()
        }
    }

    fun updatePreCiAgentId(
        dslContext: DSLContext,
        agentId: String,
        workspaceName: String
    ): Boolean {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.update(this)
                .set(PRECI_AGENT_ID, agentId)
                .where(NAME.eq(workspaceName))
                .execute() == 1
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field(
            "timestampdiff({0}, {1}, NOW())",
            Int::class.java, DSL.keyword(part.toSQL()), t1
        )
    }
}
