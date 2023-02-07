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
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
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
                /* field1 = */ PROJECT_ID,
                /* field2 = */ NAME,
                /* field3 = */ TEMPLATE_ID,
                /* field4 = */ URL,
                /* field5 = */ BRANCH,
                /* field6 = */ YAML_PATH,
                /* field7 = */ IMAGE_PATH,
                /* field8 = */ WORK_PATH,
                /* field9 = */ HOST_NAME,
                /* field10 = */ CPU,
                /* field11 = */ MEMORY,
                /* field12 = */ DISK,
                /* field13 = */ STATUS,
                /* field14 = */ LAST_STATUS_UPDATE_TIME,
                /* field15 = */ YAML,
                /* field16 = */ DOCKERFILE,
                /* field17 = */ CREATOR,
                /* field18 = */ CREATOR_BG_NAME,
                /* field19 = */ CREATOR_DEPT_NAME,
                /* field20 = */ CREATOR_CENTER_NAME
            )
                .values(
                    /* value1 = */ "",
                    /* value2 = */ workspace.workspaceName,
                    /* value3 = */ workspace.wsTemplateId,
                    /* value4 = */ workspace.repositoryUrl,
                    /* value5 = */ workspace.branch,
                    /* value6 = */ workspace.devFilePath,
                    /* value7 = */ "",
                    /* value8 = */ workspace.workPath,
                    /* value9 = */ workspace.hostName,
                    /* value10 = */ 8,
                    /* value11 = */ 16,
                    /* value12 = */ 100,
                    /* value13 = */ workspaceStatus.ordinal,
                    /* value14 = */ LocalDateTime.now(),
                    /* value15 = */ workspace.yaml,
                    /* value16 = */ "",
                    /* value17 = */ workspace.createUserId,
                    /* value18 = */ userInfo?.bgName ?: "",
                    /* value19 = */ userInfo?.deptName ?: "",
                    /* value20 = */ userInfo?.centerName ?: ""
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

            if (condition.isEmpty()) {
                return null
            }
            return dslContext.selectFrom(this)
                .where(condition).orderBy(CREATE_TIME.desc(), ID.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch()
        }
    }

    /**
     * 获得用户所拥有工作空间列表（计数）
     */
    fun countUserWorkspace(
        dslContext: DSLContext,
        userId: String? = null
    ): Long {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectCount().from(this)
                .where(CREATOR.eq(userId)).unionAll(
                    DSL.selectCount().from(this).where(
                        NAME.`in`(
                            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                                shared.SHARED_USER.eq(
                                    userId
                                )
                            )
                        )
                    )
                )
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
        workspaceName: String? = null
    ): TWorkspaceRecord? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId = userId, workspaceName = workspaceName)

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

    fun mixCondition(
        userId: String? = null,
        workspaceName: String? = null,
        status: WorkspaceStatus? = null
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
                .and(STATUS.eq(3))
                .limit(1000)
                .fetch()
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field(
            "timestampdiff({0}, {1}, NOW())",
            Int::class.java, DSL.keyword(part.toSQL()), t1
        )
    }
}
