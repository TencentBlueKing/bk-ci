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

package com.tencent.devops.dispatch.base.dao

import com.tencent.devops.model.dispatch_bcs.tables.TBcsBuildHis
import com.tencent.devops.model.dispatch_bcs.tables.records.TBcsBuildHisRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class BcsBuildHisDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: String,
        secretKey: String,
        builderName: String,
        cpu: Double,
        memory: Int,
        disk: Int,
        executeCount: Int
    ): Long {
        with(TBcsBuildHis.T_BCS_BUILD_HIS) {
            val preRecord = dslContext.selectFrom(this)
                .where(BUIDLD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .fetch()
            if (preRecord != null && preRecord.size > 0) {
                dslContext.deleteFrom(this)
                    .where(BUIDLD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .execute()
            }

            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUIDLD_ID,
                VM_SEQ_ID,
                POOL_NO,
                SECRET_KEY,
                BUILDER_NAME,
                CPU,
                MEMORY,
                DISK,
                EXECUTE_COUNT
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
                executeCount
            ).returning(ID).fetchOne()?.id ?: 1
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): Result<TBcsBuildHisRecord> {
        with(TBcsBuildHis.T_BCS_BUILD_HIS) {
            val select = dslContext.selectFrom(this)
                .where(BUIDLD_ID.eq(buildId))
            if (vmSeqId != null && vmSeqId.isNotEmpty()) {
                select.and(VM_SEQ_ID.eq(vmSeqId))
            }

            return select.fetch()
        }
    }

    fun getLatestBuildHistory(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): TBcsBuildHisRecord? {
        with(TBcsBuildHis.T_BCS_BUILD_HIS) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .orderBy(GMT_CREATE.desc())
                .fetchAny()
        }
    }

    fun updateBuilderName(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        builderName: String,
        executeCount: Int
    ) {
        with(TBcsBuildHis.T_BCS_BUILD_HIS) {
            dslContext.update(this)
                .set(BUILDER_NAME, builderName)
                .where(BUIDLD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute()
        }
    }
}
