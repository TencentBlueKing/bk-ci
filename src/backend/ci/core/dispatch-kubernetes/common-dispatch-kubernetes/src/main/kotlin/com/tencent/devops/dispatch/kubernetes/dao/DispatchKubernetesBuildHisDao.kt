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

import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchKubernetesBuildHistory
import com.tencent.devops.model.dispatch.kubernetes.tables.records.TDispatchKubernetesBuildHistoryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class DispatchKubernetesBuildHisDao {

    fun create(
        dslContext: DSLContext,
        dispatchType: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: Int,
        secretKey: String,
        builderName: String,
        cpu: Double,
        memory: String,
        disk: String,
        executeCount: Int
    ): Long {
        with(TDispatchKubernetesBuildHistory.T_DISPATCH_KUBERNETES_BUILD_HISTORY) {
            val preRecord = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .fetch()
            if (preRecord.size > 0) {
                dslContext.deleteFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .execute()
            }

            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                POOL_NO,
                SECRET_KEY,
                CONTAINER_NAME,
                CPU,
                MEMORY,
                DISK,
                EXECUTE_COUNT,
                DISPATCH_TYPE
            ).values(
                projectId,
                pipelineId,
                buildId,
                vmSeqId,
                poolNo,
                secretKey,
                builderName,
                cpu,
                memory,
                disk,
                executeCount,
                dispatchType
            ).returning(ID).fetchOne()?.id ?: 1
        }
    }

    fun get(
        dslContext: DSLContext,
        dispatchType: String,
        buildId: String,
        vmSeqId: String?
    ): Result<TDispatchKubernetesBuildHistoryRecord> {
        with(TDispatchKubernetesBuildHistory.T_DISPATCH_KUBERNETES_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(DISPATCH_TYPE.eq(dispatchType))
                .and(BUILD_ID.eq(buildId))
            if (vmSeqId != null && vmSeqId.isNotEmpty()) {
                select.and(VM_SEQ_ID.eq(vmSeqId))
            }

            return select.fetch()
        }
    }

    fun getLatestBuildHistory(
        dslContext: DSLContext,
        dispatchType: String,
        pipelineId: String,
        vmSeqId: String
    ): TDispatchKubernetesBuildHistoryRecord? {
        with(TDispatchKubernetesBuildHistory.T_DISPATCH_KUBERNETES_BUILD_HISTORY) {
            return dslContext.selectFrom(this)
                .where(DISPATCH_TYPE.eq(dispatchType))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .orderBy(CREATE_TIME.desc())
                .fetchAny()
        }
    }

    fun updateBuilderName(
        dslContext: DSLContext,
        dispatchType: String,
        buildId: String,
        vmSeqId: String,
        builderName: String,
        executeCount: Int
    ) {
        with(TDispatchKubernetesBuildHistory.T_DISPATCH_KUBERNETES_BUILD_HISTORY) {
            dslContext.update(this)
                .set(CONTAINER_NAME, builderName)
                .where(DISPATCH_TYPE.eq(dispatchType))
                .and(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute()
        }
    }
}
