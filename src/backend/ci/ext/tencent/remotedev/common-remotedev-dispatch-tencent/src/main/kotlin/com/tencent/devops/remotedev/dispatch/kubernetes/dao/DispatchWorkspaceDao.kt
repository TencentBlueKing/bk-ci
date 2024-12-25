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

package com.tencent.devops.remotedev.dispatch.kubernetes.dao

import com.tencent.devops.model.remotedev.tables.TDispatchWorkspace
import com.tencent.devops.model.remotedev.tables.records.TDispatchWorkspaceRecord
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class DispatchWorkspaceDao {

    fun createWorkspace(
        userId: String,
        event: WorkspaceCreateEvent,
        environmentUid: String,
        regionId: Int,
        taskUid: String? = "",
        dslContext: DSLContext
    ): Long {
        return with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            dslContext.insertInto(
                this,
                USER_ID,
                PROJECT_ID,
                WORKSPACE_NAME,
                ENVIRONMENT_UID,
                STATUS, /*废弃此表的STATUS*/
                REGION_ID,
                TASK_ID
            )
                .values(
                    userId,
                    event.projectId,
                    event.workspaceName,
                    environmentUid,
                    -1,
                    regionId,
                    taskUid
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun updateWorkspace(
        workspaceName: String,
        envId: String,
        regionId: Int,
        dslContext: DSLContext
    ) {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            dslContext.update(this)
                .set(ENVIRONMENT_UID, envId)
                .set(REGION_ID, regionId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(WORKSPACE_NAME.eq(workspaceName))
                .execute()
        }
    }

    fun deleteWorkspace(
        dslContext: DSLContext,
        workspaceName: String,
        bakWorkspaceName: String?
    ): Int {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            return dslContext.update(this)
                .set(WORKSPACE_NAME, bakWorkspaceName ?: "$workspaceName [deleted]")
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
}
