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

import com.tencent.devops.dispatch.kubernetes.pojo.PerformanceOptionsVO
import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchKubernetesPerformanceOption
import com.tencent.devops.model.dispatch.kubernetes.tables.records.TDispatchKubernetesPerformanceOptionRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PerformanceOptionsDao {

    fun create(
        dslContext: DSLContext,
        dispatchType: String,
        cpu: Double,
        memory: String,
        disk: String,
        description: String
    ) {
        with(TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION) {
            dslContext.insertInto(
                this,
                DISPATCH_TYPE,
                CPU,
                MEMORY,
                DISK,
                DESCRIPTION,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                dispatchType,
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
        with(TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION) {
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

    fun get(dslContext: DSLContext, id: Long): TDispatchKubernetesPerformanceOptionRecord? {
        with(TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getList(dslContext: DSLContext): Result<TDispatchKubernetesPerformanceOptionRecord> {
        with(TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun getOptionsList(
        dslContext: DSLContext,
        dispatchType: String,
        cpu: Double,
        memory: String,
        disk: String
    ): Result<TDispatchKubernetesPerformanceOptionRecord> {
        with(TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION) {
            return dslContext.selectFrom(this)
                .where(DISPATCH_TYPE.eq(dispatchType))
                .and(CPU.lessOrEqual(cpu))
                .and(MEMORY.lessOrEqual(memory))
                .and(DISK.lessOrEqual(disk))
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Int {
        return with(TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION) {
            dslContext.delete(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}
