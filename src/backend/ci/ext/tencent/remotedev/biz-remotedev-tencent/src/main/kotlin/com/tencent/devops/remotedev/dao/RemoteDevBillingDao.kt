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

import com.tencent.devops.model.remotedev.tables.TRemoteDevBilling
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Deprecated("LINUX 待删除")
class RemoteDevBillingDao {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevBillingDao::class.java)
    }

    fun fetchNotEndBilling(dslContext: DSLContext, userId: String): List<LocalDateTime> {
        val ws = TWorkspace.T_WORKSPACE
        with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            return dslContext.select(START_TIME).from(this).leftJoin(ws).on(ws.NAME.eq(this.WORKSPACE_NAME))
                .where(USER.eq(userId)).and(END_TIME.isNull).and(ws.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name))
                .fetch(START_TIME)
        }
    }

    fun fetchBillings(
        dslContext: DSLContext,
        type: WorkspaceSystemType,
        userId: String?,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): List<Triple<String, LocalDateTime, Int?>> {
        val ws = TWorkspace.T_WORKSPACE
        with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            return dslContext.select(this.USER, this.START_TIME, this.USAGE_TIME).from(this).leftJoin(ws)
                .on(ws.NAME.eq(this.WORKSPACE_NAME))
                .where(ws.SYSTEM_TYPE.eq(type.name))
                .and(ws.OWNER_TYPE.eq(ownerType.name))
                .let { if (userId != null) it.and(this.USER.eq(userId)) else it }
                .fetch().map { Triple(it.value1(), it.value2(), it.value3()) }
        }
    }
}
