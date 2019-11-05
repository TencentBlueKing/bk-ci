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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchTstackConfig
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackConfigRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TstackConfigDao {
    fun getConfig(dslContext: DSLContext, projectId: String): TDispatchTstackConfigRecord? {
        with(TDispatchTstackConfig.T_DISPATCH_TSTACK_CONFIG) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun getGreyWebConsoleProjects(dslContext: DSLContext): List<TDispatchTstackConfigRecord> {
        with(TDispatchTstackConfig.T_DISPATCH_TSTACK_CONFIG) {
            return dslContext.selectFrom(this)
                    .where(TSTACK_ENABLE.eq(true))
                    .fetch()
        }
    }

    fun saveConfig(
        dslContext: DSLContext,
        projectId: String,
        tstackEnabled: Boolean
    ) {
        with(TDispatchTstackConfig.T_DISPATCH_TSTACK_CONFIG) {
            dslContext.transaction { configuration ->
                val context = org.jooq.impl.DSL.using(configuration)
                val record = context.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .fetchOne()
                val now = java.time.LocalDateTime.now()
                if (record == null) {
                    context.insertInto(this, PROJECT_ID, TSTACK_ENABLE)
                            .values(projectId, tstackEnabled)
                            .execute()
                } else {
                    context.update(this)
                            .set(TSTACK_ENABLE, tstackEnabled)
                            .where(PROJECT_ID.eq(projectId))
                            .execute()
                }
            }
        }
    }
}