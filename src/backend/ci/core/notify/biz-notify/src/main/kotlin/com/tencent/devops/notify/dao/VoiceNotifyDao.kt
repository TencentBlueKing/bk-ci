package com.tencent.devops.notify.dao

import com.tencent.devops.model.notify.tables.TNotifyVoice
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class VoiceNotifyDao {
    fun insertOrUpdate(
        dslContext: DSLContext,
        id: String,
        success: Boolean,
        receivers: String,
        taskName: String,
        content: String,
        transferReceiver: String,
        retryCount: Int,
        lastError: String?,
        tofSysId: String?,
        fromSysId: String?
    ) {
        val now = LocalDateTime.now()
        with(TNotifyVoice.T_NOTIFY_VOICE) {
            dslContext.insertInto(
                this,
                ID,
                SUCCESS,
                RECEIVERS,
                TASK_NAME,
                CONTENT,
                TRANSFER_RECEIVER,
                RETRY_COUNT,
                LAST_ERROR,
                TOF_SYS_ID,
                FROM_SYS_ID,
                CREATED_TIME,
                UPDATED_TIME
            ).values(
                id,
                success,
                receivers,
                taskName,
                content,
                transferReceiver,
                retryCount,
                lastError,
                tofSysId,
                fromSysId,
                now,
                now
            ).onDuplicateKeyUpdate()
                .set(SUCCESS, success)
                .set(UPDATED_TIME, now)
                .set(LAST_ERROR, lastError)
                .set(RETRY_COUNT, retryCount)
                .execute()
        }
    }
}
