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

import com.tencent.devops.model.remotedev.tables.TDailyCgsData
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class WorkspaceDailyCgsdataDao {
    // 备份个人和团队云桌面快照数据
    fun backupDailyCsgData(dslContext: DSLContext) {
        val cgsList = fetchDailyCgsData(dslContext)
        if (cgsList.isNullOrEmpty()) {
            return
        }
        dslContext.batch(
            cgsList.map {
                with(TDailyCgsData.T_DAILY_CGS_DATA) {
                    dslContext.insertInto(
                        this,
                        DATE,
                        OWNER_TYPE,
                        NUMBER,
                        CREATE_TIME
                    ).values(
                        it["CUR_DATE"] as String,
                        it["OWNER_TYPE"] as String,
                        it["VALUE"] as Int,
                        LocalDateTime.now()
                    ).onDuplicateKeyIgnore()
                }
            }
        ).execute()
    }

    fun fetchDailyCgsData(
        dslContext: DSLContext
    ): Result<out Record>? {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.select(
                OWNER_TYPE, DSL.count(ID).`as`("VALUE"),
                DSL.field("DATE_FORMAT(CURDATE(), '%Y-%m-%d')").`as`("CUR_DATE")
            ).from(this)
                .where(SYSTEM_TYPE.eq(WorkspaceSystemType.WINDOWS_GPU.name))
                .and(
                    STATUS.notIn(
                        WorkspaceStatus.DELETED.ordinal,
                        WorkspaceStatus.PREPARING.ordinal,
                        WorkspaceStatus.DELIVERING.ordinal,
                        WorkspaceStatus.DELIVERING_FAILED.ordinal
                    )
                )
                .groupBy(OWNER_TYPE)
                .fetch()
        }
    }
}
