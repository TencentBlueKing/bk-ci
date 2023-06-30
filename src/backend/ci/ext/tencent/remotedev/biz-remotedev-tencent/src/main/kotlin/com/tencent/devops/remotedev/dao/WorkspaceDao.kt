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
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class WorkspaceDao {

    fun createWorkspace(
        userId: String,
        workspace: Workspace,
        workspaceStatus: WorkspaceStatus,
        userInfo: UserDeptDetail?,
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
                SYSTEM_TYPE
            )
                .values(
                    "",
                    workspace.workspaceName,
                    workspace.wsTemplateId,
                    workspace.repositoryUrl,
                    workspace.branch,
                    workspace.devFilePath,
                    "",
                    workspace.workPath,
                    workspace.workspaceFolder,
                    workspace.hostName,
                    8,
                    32,
                    100,
                    workspaceStatus.ordinal,
                    LocalDateTime.now(),
                    workspace.yaml,
                    "",
                    workspace.createUserId,
                    userInfo?.bgName ?: "",
                    userInfo?.deptName ?: "",
                    userInfo?.centerName ?: "",
                    userInfo?.groupName ?: "",
                    workspace.workspaceMountType.name,
                    workspace.workspaceSystemType.name
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
        userId: String,
        unionShared: Boolean = true,
        status: Set<WorkspaceStatus>? = null,
        systemType: WorkspaceSystemType? = null
    ): Long {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectCount().from(this)
                .where(CREATOR.eq(userId))
                .let {
                    if (status.isNullOrEmpty()) {
                        it.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                    } else {
                        it.and(STATUS.`in`(status.map { s -> s.ordinal }))
                    }
                }
                .let { if (systemType != null) it.and(SYSTEM_TYPE.eq(systemType.name)) else it }
                .let {
                    if (unionShared) it.unionAll(
                        DSL.selectCount().from(this).where(
                            NAME.`in`(
                                DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                                    shared.SHARED_USER.eq(
                                        userId
                                    )
                                )
                            )
                        ).let { i -> if (systemType != null) i.and(SYSTEM_TYPE.eq(systemType.name)) else i }
                    ) else it
                }
                .fetch(0, Long::class.java).sum()
        }
    }

    /**
     * 获得用户所拥有工作空间列表
     */
    fun limitFetchUserWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        userId: String
    ): Result<TWorkspaceRecord>? {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(CREATOR.eq(userId))
                .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .unionAll(
                    DSL.selectFrom(this).where(
                        NAME.`in`(
                            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                                shared.SHARED_USER.eq(
                                    userId
                                )
                            )
                        )
                    )
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
        status: WorkspaceStatus? = null
    ): Result<TWorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId = userId, status = status)

            if (condition.isEmpty()) {
                return null
            }

            return dslContext.selectFrom(this)
                .where(condition)
                .fetch()
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
                .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
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
                .execute()
        }
    }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun getTimeOutInactivityWorkspace(
        timeOutDays: Int,
        dslContext: DSLContext
    ): Result<TWorkspaceRecord> {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(
                    timestampDiff(DatePart.DAY, UPDATE_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(
                        timeOutDays
                    )
                )
                .and(STATUS.eq(WorkspaceStatus.SLEEP.ordinal))
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
