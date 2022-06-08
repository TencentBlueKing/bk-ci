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

import com.tencent.devops.dispatch.bcs.pojo.performance.PerformanceOptionsVO
import com.tencent.devops.model.dispatch_bcs.tables.TBcsPerformanceOptions
import com.tencent.devops.model.dispatch_bcs.tables.records.TBcsPerformanceOptionsRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DcPerformanceOptionsDao {

    fun create(
        dslContext: DSLContext,
        cpu: Double,
        memory: Int,
        disk: Int,
        description: String
    ) {
        with(TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS) {
            dslContext.insertInto(
                this,
                CPU,
                MEMORY,
                DISK,
                DESCRIPTION,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                cpu,
                memory,
                disk,
                description,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        performanceOptionsVO: PerformanceOptionsVO
    ) {
        with(TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS) {
            dslContext.update(this)
                .set(CPU, performanceOptionsVO.cpu)
                .set(MEMORY, performanceOptionsVO.memory)
                .set(DISK, performanceOptionsVO.disk)
                .set(DESCRIPTION, performanceOptionsVO.description)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, id: Long): TBcsPerformanceOptionsRecord? {
        with(TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getList(dslContext: DSLContext): Result<TBcsPerformanceOptionsRecord> {
        with(TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun getOptionsList(
        dslContext: DSLContext,
        cpu: Double,
        memory: Int,
        disk: Int
    ): Result<TBcsPerformanceOptionsRecord> {
        with(TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS) {
            return dslContext.selectFrom(this)
                .where(CPU.lessOrEqual(cpu))
                .and(MEMORY.lessOrEqual(memory))
                .and(DISK.lessOrEqual(disk))
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Int {
        return with(TBcsPerformanceOptions.T_BCS_PERFORMANCE_OPTIONS) {
            dslContext.delete(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}
