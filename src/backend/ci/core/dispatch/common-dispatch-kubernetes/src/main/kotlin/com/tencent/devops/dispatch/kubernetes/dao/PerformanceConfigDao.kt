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

import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchKubernetesPerformanceConfig
import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchKubernetesPerformanceOption
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record5
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PerformanceConfigDao {

    fun createOrUpdate(
        dslContext: DSLContext,
        dispatchType: String,
        projectId: String,
        optionId: Long
    ) {
        with(TDispatchKubernetesPerformanceConfig.T_DISPATCH_KUBERNETES_PERFORMANCE_CONFIG) {
            dslContext.insertInto(
                this,
                DISPATCH_TYPE,
                PROJECT_ID,
                OPTION_ID,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                dispatchType,
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
        dispatchType: String,
        projectId: String
    ): Record? {
        val t1 = TDispatchKubernetesPerformanceConfig.T_DISPATCH_KUBERNETES_PERFORMANCE_CONFIG.`as`("t1")
        val t2 = TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION.`as`("t2")
        return dslContext.select(t1.PROJECT_ID, t2.CPU, t2.MEMORY, t2.DISK)
            .from(t1).leftJoin(t2).on(t1.OPTION_ID.eq(t2.ID))
            .where(t1.DISPATCH_TYPE.eq(dispatchType))
            .and(t1.PROJECT_ID.eq(projectId))
            .fetchOne()
    }

    fun getList(
        dslContext: DSLContext,
        dispatchType: String,
        page: Int,
        pageSize: Int
    ): Result<Record5<String, Double, String, String, String>>? {
        val t1 = TDispatchKubernetesPerformanceConfig.T_DISPATCH_KUBERNETES_PERFORMANCE_CONFIG.`as`("t1")
        val t2 = TDispatchKubernetesPerformanceOption.T_DISPATCH_KUBERNETES_PERFORMANCE_OPTION.`as`("t2")
        return dslContext.select(t1.PROJECT_ID, t2.CPU, t2.MEMORY, t2.DISK, t2.DESCRIPTION)
            .from(t1).leftJoin(t2).on(t1.OPTION_ID.eq(t2.ID))
            .where(t1.DISPATCH_TYPE.eq(dispatchType))
            .limit(pageSize).offset((page - 1) * pageSize)
            .fetch()
    }

    fun getCount(
        dslContext: DSLContext,
        dispatchType: String
    ): Long {
        with(TDispatchKubernetesPerformanceConfig.T_DISPATCH_KUBERNETES_PERFORMANCE_CONFIG) {
            return dslContext.selectCount()
                .from(this)
                .where(DISPATCH_TYPE.eq(dispatchType))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        dispatchType: String,
        projectId: String
    ): Int {
        return with(TDispatchKubernetesPerformanceConfig.T_DISPATCH_KUBERNETES_PERFORMANCE_CONFIG) {
            dslContext.delete(this)
                .where(DISPATCH_TYPE.eq(dispatchType))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}
