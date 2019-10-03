package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchTstackVolume
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackVolumeRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TstackVolumeDao {
    fun insertVolume(
        dslContext: DSLContext,
        volumeId: String,
        pipelineId: String,
        vmSeqId: String
    ) {
        with(TDispatchTstackVolume.T_DISPATCH_TSTACK_VOLUME) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                    VOLUME_ID,
                    PIPELINE_ID,
                    VM_SEQ_ID,
                    CREATED_TIME,
                    UPDATED_TIME
            ).values(
                    volumeId,
                    pipelineId,
                    vmSeqId,
                    now,
                    now
            ).execute()
        }
    }

    fun getVolume(dslContext: DSLContext, pipelineId: String, vmSeqId: String): TDispatchTstackVolumeRecord? {
        with(TDispatchTstackVolume.T_DISPATCH_TSTACK_VOLUME) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()
        }
    }

    fun deleteVolume(dslContext: DSLContext, volumeId: String) {
        // todo
    }
}