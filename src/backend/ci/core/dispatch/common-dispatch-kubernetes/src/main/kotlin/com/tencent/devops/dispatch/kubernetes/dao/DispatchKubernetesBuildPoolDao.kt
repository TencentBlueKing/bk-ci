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

import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchKubernetesBuildPool
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DispatchKubernetesBuildPoolDao {

    fun setBaseBuildLastBuilder(
        dslContext: DSLContext,
        dispatchType: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        builderName: String,
        poolNo: Int
    ) {
        with(TDispatchKubernetesBuildPool.T_DISPATCH_KUBERNETES_BUILD_POOL) {
            dslContext.insertInto(
                this,
                DISPATCH_TYPE,
                BUILD_ID,
                VM_SEQ_ID,
                EXECUTE_COUNT,
                CONTAINER_NAME,
                POOL_NO,
                CREATE_TIME
            ).values(
                dispatchType,
                buildId,
                vmSeqId,
                executeCount,
                builderName,
                poolNo,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getBaseBuildLastBuilder(
        dslContext: DSLContext,
        dispatchType: String,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TDispatchKubernetesBuildPool.T_DISPATCH_KUBERNETES_BUILD_POOL) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(DISPATCH_TYPE.eq(dispatchType))
                    .and(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records.forEach {
                    result.add(Pair(it.vmSeqId, it.containerName))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(DISPATCH_TYPE.eq(dispatchType))
                    .and(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()
                result.add(Pair(vmSeqId, record?.containerName))
            }
        }
        return result
    }

    fun getBaseBuildLastPoolNo(
        dslContext: DSLContext,
        dispatchType: String,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, Int?>> {
        val result = mutableListOf<Pair<String, Int?>>()
        with(TDispatchKubernetesBuildPool.T_DISPATCH_KUBERNETES_BUILD_POOL) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(DISPATCH_TYPE.eq(dispatchType))
                    .and(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records.forEach {
                    result.add(Pair(it.vmSeqId, it.poolNo))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(DISPATCH_TYPE.eq(dispatchType))
                    .and(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()

                result.add(Pair(vmSeqId, record?.poolNo))
            }
        }
        return result
    }

    fun deleteBaseBuildLastBuilderPoolNo(
        dslContext: DSLContext,
        dispatchType: String,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ) {
        with(TDispatchKubernetesBuildPool.T_DISPATCH_KUBERNETES_BUILD_POOL) {
            if (vmSeqId == null) {
                dslContext.delete(this)
                    .where(DISPATCH_TYPE.eq(dispatchType))
                    .and(BUILD_ID.eq(buildId))
                    .execute()
            } else {
                dslContext.delete(this)
                    .where(DISPATCH_TYPE.eq(dispatchType))
                    .and(BUILD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .execute()
            }
        }
    }
}
