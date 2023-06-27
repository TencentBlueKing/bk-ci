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

import com.tencent.devops.model.environment.tables.TBkbizProject
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
        bpList: List<Pair<String, Long>>,
        bizId: Long,
        projectId: String
    ): Boolean {
        if (bpList.isEmpty()) {
            return false
        }
        with(TBkbizProject.T_BKBIZ_PROJECT) {
            val sql = dslContext.insertInto(this)
                .set(PROJECT_ID, bpList.first().first)
                .set(BIZ_ID, bpList.first().second)
            if (bpList.size <= 1) {
                return sql.execute() > 0
            }
            bpList.forEach { (p, b) ->
                sql.newRecord().set(PROJECT_ID, p).set(BIZ_ID, b)
            }
            return sql.execute() > 0
        }
    }
}
