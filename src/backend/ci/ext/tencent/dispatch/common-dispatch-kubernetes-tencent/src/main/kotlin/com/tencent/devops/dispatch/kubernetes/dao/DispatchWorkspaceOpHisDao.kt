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

import com.tencent.devops.dispatch.kubernetes.pojo.DispatchWorkspaceOpHisRecord
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchWorkspaceOpHis
import com.tencent.devops.model.dispatch.kubernetes.tables.records.TDispatchWorkspaceOpHisRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
class DispatchWorkspaceOpHisDao {

    fun createWorkspaceHistory(
        dslContext: DSLContext,
        workspaceName: String,
        environmentUid: String,
        operator: String,
        action: EnvironmentAction,
        uid: String,
        actionMsg: String = ""
    ) {
        with(TDispatchWorkspaceOpHis.T_DISPATCH_WORKSPACE_OP_HIS) {
            dslContext.insertInto(
                this,
                WORKSPACE_NAME,
                ENVIRONMENT_UID,
                OPERATOR,
                ACTION,
                ACTION_MSG,
                CREATED_TIME,
                STATUS,
                UID
            )
                .values(
                    workspaceName,
                    environmentUid,
                    operator,
                    action.name,
                    actionMsg.take(255),
                    LocalDateTime.now(),
                    EnvironmentActionStatus.PENDING.name,
                    uid
                ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        uid: String,
        status: EnvironmentActionStatus,
        fStatus: EnvironmentActionStatus? = null,
        workspaceName: String? = null,
        actionMsg: String? = null
    ): Int {
        with(TDispatchWorkspaceOpHis.T_DISPATCH_WORKSPACE_OP_HIS) {
            return dslContext.update(this).set(STATUS, status.name)
                .let {
                    if (actionMsg != null) it.set(ACTION_MSG, actionMsg.take(255)) else it
                }
                .let {
                    if (workspaceName != null) it.set(WORKSPACE_NAME, workspaceName) else it
                }
                .where(UID.eq(uid))
                .let {
                    if (fStatus != null) it.and(STATUS.eq(fStatus.name)) else it
                }.execute()
        }
    }

    fun getTask(
        dslContext: DSLContext,
        uid: String
    ): DispatchWorkspaceOpHisRecord? {
        with(TDispatchWorkspaceOpHis.T_DISPATCH_WORKSPACE_OP_HIS) {
            return dslContext.selectFrom(this)
                .where(UID.eq(uid)).orderBy(ID.desc()).fetchAny(mapper)
        }
    }

    fun updateStatusByWorkspaceName(
        dslContext: DSLContext,
        workspaceName: String,
        status: EnvironmentActionStatus,
        fStatus: EnvironmentActionStatus
    ): Int {
        with(TDispatchWorkspaceOpHis.T_DISPATCH_WORKSPACE_OP_HIS) {
            return dslContext.update(this)
                .set(STATUS, status.name)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(STATUS.eq(fStatus.name)).execute()
        }
    }

    class RecordJooqMapper : RecordMapper<TDispatchWorkspaceOpHisRecord, DispatchWorkspaceOpHisRecord> {
        override fun map(record: TDispatchWorkspaceOpHisRecord?): DispatchWorkspaceOpHisRecord? {
            return record?.run {
                DispatchWorkspaceOpHisRecord(
                    workspaceName = workspaceName,
                    envId = environmentUid,
                    operator = operator,
                    action = EnvironmentAction.valueOf(action),
                    actionMsg = actionMsg,
                    createTime = createdTime,
                    uid = uid,
                    status = EnvironmentActionStatus.parse(status)
                )
            }
        }
    }

    companion object {
        val mapper = RecordJooqMapper()
    }
}
