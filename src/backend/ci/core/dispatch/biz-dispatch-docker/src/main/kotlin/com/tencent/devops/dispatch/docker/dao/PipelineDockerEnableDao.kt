/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.docker.dao

import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerEnable
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerEnableRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PipelineDockerEnableDao {

    fun enable(dslContext: DSLContext, pipelineId: String, vmSeqId: Int?, enable: Boolean) {
        val seqId = vmSeqId ?: -1
        with(TDispatchPipelineDockerEnable.T_DISPATCH_PIPELINE_DOCKER_ENABLE) {
            val enableRecord = dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(PIPELINE_ID))
                    .and(VM_SEQ_ID.eq(seqId))
                    .fetchOne()
            if (enableRecord != null) {
                if (ByteUtils.byte2Bool(enableRecord.enable) == enable) {
                    return
                }
                dslContext.update(this)
                        .set(ENABLE, ByteUtils.bool2Byte(enable))
                        .where(PIPELINE_ID.eq(PIPELINE_ID))
                        .and(VM_SEQ_ID.eq(seqId))
                        .execute()
            } else {
                dslContext.insertInto(this,
                        PIPELINE_ID,
                        VM_SEQ_ID,
                        ENABLE)
                        .values(
                                pipelineId,
                                seqId,
                                ByteUtils.bool2Byte(enable)
                        )
                        .execute()
            }
        }
    }

    fun enable(dslContext: DSLContext, pipelineId: String, vmSeqId: Int?): Boolean {
        val seqId = vmSeqId ?: -1

        with(TDispatchPipelineDockerEnable.T_DISPATCH_PIPELINE_DOCKER_ENABLE) {
            return ByteUtils.byte2Bool(dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(VM_SEQ_ID.eq(seqId))
                    .fetchOne()?.enable ?: 0)
        }
    }

    fun list(dslContext: DSLContext): Result<TDispatchPipelineDockerEnableRecord> {
        with(TDispatchPipelineDockerEnable.T_DISPATCH_PIPELINE_DOCKER_ENABLE) {
            return dslContext.selectFrom(this).fetch()
        }
    }
}
