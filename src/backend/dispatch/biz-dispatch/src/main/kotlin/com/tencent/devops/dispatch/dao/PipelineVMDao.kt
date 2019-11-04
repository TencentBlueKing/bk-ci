/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineVm
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PipelineVMDao {

    fun getVMs(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: Int?
    ): String? {
        with(TDispatchPipelineVm.T_DISPATCH_PIPELINE_VM) {
            val step = dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
            if (vmSeqId != null) {
                step.and(VM_SEQ_ID.eq(vmSeqId))
            } else {
                step.and(VM_SEQ_ID.eq(-1))
            }
            return step.fetchOne()?.vmNames
        }
    }

    fun setVMs(
        dslContext: DSLContext,
        pipelineId: String,
        vmNames: String,
        vmSeqId: Int?
    ) {
        with(TDispatchPipelineVm.T_DISPATCH_PIPELINE_VM) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val step = context.selectFrom(this)
                        .where(PIPELINE_ID.eq(pipelineId))
                if (vmSeqId != null) {
                    step.and(VM_SEQ_ID.eq(vmSeqId))
                }
                val exist = step.fetch()

                if (exist.isEmpty()) {
                    // insert
                    if (vmSeqId == null) {
                        context.insertInto(this,
                                PIPELINE_ID,
                                VM_NAMES)
                                .values(pipelineId, vmNames)
                                .execute()
                    } else {
                        context.insertInto(this,
                                PIPELINE_ID,
                                VM_NAMES,
                                VM_SEQ_ID)
                                .values(pipelineId, vmNames, vmSeqId)
                                .execute()
                    }
                } else {
                    // update
                    val updateStep = context.update(this)
                            .set(VM_NAMES, vmNames)
                            .where(PIPELINE_ID.eq(pipelineId))

                    if (vmSeqId != null) {
                        updateStep.and(VM_SEQ_ID.eq(vmSeqId))
                    }

                    updateStep.execute()
                }
            }
        }
    }
}