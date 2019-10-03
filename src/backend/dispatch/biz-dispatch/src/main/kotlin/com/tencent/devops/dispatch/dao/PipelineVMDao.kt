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