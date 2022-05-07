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

package com.tencent.devops.dispatch.bcs.dao

import com.tencent.devops.model.dispatch_bcs.tables.TBcsPerformanceConfig
import com.tencent.devops.model.dispatch_bcs.tables.TBcsPerformanceOptions
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record5
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BcsPerformanceConfigDao {

    fun createOrUpdate(
        dslContext: DSLContext,
        projectId: String,
        optionId: Long
    ) {
        with(TBcsPerformanceConfig.T_BCS_PERFORMANCE_CONFIG) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                OPTION_ID,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                projectId,
                optionId,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(OPTION_ID, optionId)
                .execute()
        }
    }

    fun getByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): Record? {
        val t1 = TBcsPerformanceConfig.T_BCS_PERFORMANCE_CONFIG.`as`("t1")
        val t2 = TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS.`as`("t2")
        return dslContext.select(t1.PROJECT_ID, t2.CPU, t2.MEMORY, t2.DISK)
            .from(t1).leftJoin(t2).on(t1.OPTION_ID.eq(t2.ID))
            .where(t1.PROJECT_ID.eq(projectId))
            .fetchOne()
    }

    fun getList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record5<String, Double, Int, Int, String>>? {
        val t1 = TBcsPerformanceConfig.T_BCS_PERFORMANCE_CONFIG.`as`("t1")
        val t2 = TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS.`as`("t2")
        return dslContext.select(t1.PROJECT_ID, t2.CPU, t2.MEMORY, t2.DISK, t2.DESCRIPTION)
            .from(t1).leftJoin(t2).on(t1.OPTION_ID.eq(t2.ID))
            .limit(pageSize).offset((page - 1) * pageSize)
            .fetch()
    }

    fun getCount(
        dslContext: DSLContext
    ): Long {
        with(TBcsPerformanceConfig.T_BCS_PERFORMANCE_CONFIG) {
            return dslContext.selectCount()
                .from(this)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        return with(TBcsPerformanceConfig.T_BCS_PERFORMANCE_CONFIG) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}
