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

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.pojo.PipelineBuild
import com.tencent.devops.dispatch.pojo.PipelineBuildCreate
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineBuildRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DispatchPipelineBuildDao {

    fun exist(dslContext: DSLContext, buildId: String, vmSeqId: String): Boolean {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .fetch().isNotEmpty
        }
    }

    fun add(dslContext: DSLContext, pipelineBuild: PipelineBuildCreate) {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                VM_ID,
                CREATED_TIME,
                UPDATED_TIME,
                STATUS
            )
                .values(
                    pipelineBuild.projectId,
                    pipelineBuild.pipelineId,
                    pipelineBuild.buildId,
                    pipelineBuild.vmSeqId,
                    pipelineBuild.vmId,
                    now,
                    now,
                    PipelineTaskStatus.QUEUE.status
                )
                .execute()
        }
    }

    fun updatePipelineStatus(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        vmId: Int,
        status: PipelineTaskStatus
    ): Boolean {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.update(this)
                .set(VM_ID, vmId)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun updatePipelineStatus(dslContext: DSLContext, id: Int, status: PipelineTaskStatus): Boolean {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute() == 1
        }
    }

    fun listByPipelineAndVmSeqId(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        limit: Int
    ): Result<TDispatchPipelineBuildRecord> {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(STATUS.eq(PipelineTaskStatus.DONE.status))
                .orderBy(CREATED_TIME.desc())
                .limit(limit)
                .fetch()
        }
    }

    fun getPipelineByBuildIdOrNull(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): List<TDispatchPipelineBuildRecord> {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            val context = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))

            if (vmSeqId != null) {
                context.and(VM_SEQ_ID.eq(vmSeqId))
            }
            return context.fetch()
        }
    }

    fun convert(record: TDispatchPipelineBuildRecord): PipelineBuild {
        with(record) {
            return PipelineBuild(
                projectId,
                pipelineId,
                buildId,
                vmSeqId,
                vmId,
                createdTime.timestamp(),
                status
            )
        }
    }
}