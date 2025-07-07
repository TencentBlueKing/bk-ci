package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthSyncDataTask
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthSyncDataTaskDao {
    fun recordSyncDataTask(
        dslContext: DSLContext,
        taskId: String,
        taskType: String
    ) {
        with(TAuthSyncDataTask.T_AUTH_SYNC_DATA_TASK) {
            dslContext.insertInto(
                this,
                TASK_ID,
                TASK_TYPE,
                START_TIME
            ).values(
                taskId,
                taskType,
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(END_TIME, LocalDateTime.now())
                .execute()
        }
    }
}
