package com.tencent.devops.dispatch.dao

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