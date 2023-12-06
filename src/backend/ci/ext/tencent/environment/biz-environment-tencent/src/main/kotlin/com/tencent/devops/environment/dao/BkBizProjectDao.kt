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

package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.BizProjectItem
import com.tencent.devops.model.environment.tables.TBkbizProject
import com.tencent.devops.model.environment.tables.records.TBkbizProjectRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class BkBizProjectDao {
    fun add(
        dslContext: DSLContext,
        bizId: Long,
        projectId: String
    ): Boolean {
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            return dslContext.insertInto(this).columns(BIZ_ID, PROJECT_ID).values(bizId, projectId).execute() > 0
        }
    }

    fun fetchBizId(
        dslContext: DSLContext,
        projectId: String
    ): Long? {
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetchAny()?.bizId
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        bpList: List<BizProjectItem>
    ): Boolean {
        if (bpList.isEmpty()) {
            return false
        }
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            val sql = dslContext.insertInto(this)
                .set(PROJECT_ID, bpList.first().projectId)
                .set(BIZ_ID, bpList.first().bkBizId)
            if (bpList.size <= 1) {
                return sql.execute() > 0
            }
            bpList.forEach {
                sql.newRecord().set(PROJECT_ID, it.projectId).set(BIZ_ID, it.bkBizId)
            }
            return sql.execute() > 0
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Boolean {
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            return dslContext.deleteFrom(this).where(ID.eq(id)).execute() > 0
        }
    }

    fun fetchRecord(
        dslContext: DSLContext,
        projectId: String
    ): TBkbizProjectRecord? {
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetchAny()
        }
    }

    fun updateDashboard(
        dslContext: DSLContext,
        projectId: String,
        update: Boolean
    ): Boolean {
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            return dslContext.update(this)
                .set(ENABLE_MONITOR_DASHBOARD, update)
                .where(PROJECT_ID.eq(projectId))
                .execute() > 0
        }
    }
}
