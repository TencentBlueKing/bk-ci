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

package com.tencent.devops.dispatch.kubernetes.dao

import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.model.dispatch_kubernetes.tables.TDispatchWorkspace
import com.tencent.devops.model.dispatch_kubernetes.tables.records.TDispatchWorkspaceRecord
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class DispatchWorkspaceDao {

    fun createWorkspace(
        userId: String,
        event: WorkspaceCreateEvent,
        environmentUid: String,
        status: EnvStatusEnum,
        dslContext: DSLContext
    ): Long {
        return with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                WORKSPACE_NAME,
                ENVIRONMENT_UID,
                GIT_URL,
                BRANCH,
                IMAGE,
                STATUS,
            )
                .values(
                    "",
                    event.workspaceName,
                    environmentUid,
                    event.repositoryUrl,
                    event.branch,
                    event.devFile.image?.publicImage ?: "",
                    status.ordinal
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun updateWorkspaceStatus(
        workspaceName: String,
        status: EnvStatusEnum,
        dslContext: DSLContext
    ) {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            dslContext.update(this)
                .set(STATUS, status.ordinal)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(WORKSPACE_NAME.eq(workspaceName))
                .execute()
        }
    }

    fun deleteWorkspace(
        workspaceName: String,
        dslContext: DSLContext
    ): Int {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            return dslContext.delete(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .execute()
        }
    }

    fun getWorkspaceInfo(
        workspaceName: String,
        dslContext: DSLContext
    ): TDispatchWorkspaceRecord? {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .fetchOne()
        }
    }

    fun getNoUseIdleWorkspace(dslContext: DSLContext): Result<TDispatchWorkspaceRecord> {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.DAY, UPDATE_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(7))
                .and(STATUS.eq(0))
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
