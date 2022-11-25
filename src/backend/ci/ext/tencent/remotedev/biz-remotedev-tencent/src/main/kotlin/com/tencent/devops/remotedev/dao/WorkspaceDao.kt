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
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WorkspaceDao {

    fun createWorkspace(
        userId: String,
        workspace: Workspace,
        workspaceStatus: WorkspaceStatus,
        dslContext: DSLContext
    ): Long {
        return with(TWorkspace.T_WORKSPACE) {
            dslContext.insertInto(
                this,
                USER_ID,
                PROJECT_ID,
                NAME,
                TEMPLATE_ID,
                URL,
                BRANCH,
                YAML_PATH,
                IMAGE_PATH,
                CPU,
                MEMORY,
                DISK,
                STATUS
            )
                .values(
                    userId,
                    "",
                    workspace.name,
                    workspace.wsTemplateId,
                    workspace.repositoryUrl,
                    workspace.branch,
                    workspace.devFilePath,
                    "",
                    8,
                    16,
                    100,
                    workspaceStatus.ordinal
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun countWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
    ): Long {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId = userId)
            return dslContext.selectCount().from(this)
                .where(condition)
                .fetchOne(0, Long::class.java) ?: 0
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
                .where(condition).orderBy(CREATE_TIME.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch()
        }
    }

    fun updateWorkspaceName(
        workspaceId: Long,
        name: String,
        status: WorkspaceStatus,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(STATUS, status.ordinal)
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
}
