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
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
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
                /* field1 = */ USER_ID,
                /* field2 = */ PROJECT_ID,
                /* field3 = */ NAME,
                /* field4 = */ TEMPLATE_ID,
                /* field5 = */ URL,
                /* field6 = */ BRANCH,
                /* field7 = */ YAML_PATH,
                /* field8 = */ IMAGE_PATH,
                /* field9 = */ CPU,
                /* field10 = */ MEMORY,
                /* field11 = */ DISK,
                /* field12 = */ STATUS,
                /* field13 = */ LAST_STATUS_UPDATE_TIME,
                /* field14 = */ YAML,
                /* field15 = */ DOCKERFILE,
                /* field16 = */ CREATOR,
                /* field17 = */ CREATOR_BG_NAME,
                /* field18 = */ CREATOR_DEPT_NAME,
                /* field19 = */ CREATOR_CENTER_NAME
            )
                .values(
                    /* value1 = */ userId,
                    /* value2 = */ "",
                    /* value3 = */ workspace.name,
                    /* value4 = */ workspace.wsTemplateId,
                    /* value5 = */ workspace.repositoryUrl,
                    /* value6 = */ workspace.branch,
                    /* value7 = */ workspace.devFilePath,
                    /* value8 = */ "",
                    /* value9 = */ 8,
                    /* value10 = */ 16,
                    /* value11 = */ 100,
                    /* value12 = */ workspaceStatus.ordinal,
                    /* value13 = */ LocalDateTime.now(),
                    /* value14 = */ workspace.yaml,
                    /* value15 = */ "",
                    /* value16 = */ workspace.createUserId,
                    /* value17 = */ userInfo?.bgName ?: "",
                    /* value18 = */ userInfo?.deptName ?: "",
                    /* value19 = */ userInfo?.centerName ?: ""
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun limitFetchWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        userId: String? = null,
        workspaceId: Long? = null
    ): Result<TWorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId, workspaceId)

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
        userId: String? = null,
    ): Long {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectCount().from(this)
                .where(USER_ID.eq(userId)).unionAll(
                    DSL.selectCount().from(this).where(
                        ID.`in`(
                            DSL.select(shared.WORKSPACE_ID).from(shared).where(
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
                .where(USER_ID.eq(userId)).unionAll(
                    DSL.selectFrom(this).where(
                        ID.`in`(
                            DSL.select(shared.WORKSPACE_ID).from(shared).where(
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

    fun updateWorkspaceName(
        workspaceId: Long,
        name: String,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(NAME, name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(workspaceId))
                .execute()
        }
    }

    fun fetchAnyWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
        workspaceId: Long? = null
    ): TWorkspaceRecord? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId, workspaceId)

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
        userId: String? = null
    ): Result<TWorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId)

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
        workspaceId: Long? = null
    ): List<Condition> {
        val condition = mutableListOf<Condition>()
        with(TWorkspace.T_WORKSPACE) {
            if (!userId.isNullOrBlank()) {
                condition.add(USER_ID.eq(userId))
            }
            if (workspaceId != null) {
                condition.add(ID.eq(workspaceId))
            }
        }
        return condition
    }

    fun updateWorkspaceStatus(
        workspaceId: Long,
        status: WorkspaceStatus,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(STATUS, status.ordinal)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(workspaceId))
                .execute()
        }
    }

    fun updateWorkspaceUsageTime(
        workspaceId: Long,
        usageTime: Int,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(USAGE_TIME, USAGE_TIME + usageTime)
                .where(ID.eq(workspaceId))
                .execute()
        }
    }

    fun updateWorkspaceSleepingTime(
        workspaceId: Long,
        sleepTime: Int,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(SLEEPING_TIME, SLEEPING_TIME + sleepTime)
                .where(ID.eq(workspaceId))
                .execute()
        }
    }

    fun deleteWorkspace(
        workspaceId: Long,
        dslContext: DSLContext
    ): Int {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.delete(this)
                .where(ID.eq(workspaceId))
                .execute()
        }
    }
}
