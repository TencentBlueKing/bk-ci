package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.model.auth.tables.TAuthTemporaryVerifyRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthVerifyRecordDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        VerifyRecordDTO: VerifyRecordDTO
    ) {
        val now = LocalDateTime.now()
        with(TAuthTemporaryVerifyRecord.T_AUTH_TEMPORARY_VERIFY_RECORD) {
            dslContext.insertInto(
                this,
                USER_ID,
                PROJECT_CODE,
                RESOURCE_TYPE,
                RESOURCE_CODE,
                ACTION,
                ENVIRONMENT_TAG,
                VERIFY_RESULT,
                LAST_VERIFY_TIME
            ).values(
                VerifyRecordDTO.userId,
                VerifyRecordDTO.projectId,
                VerifyRecordDTO.resourceType,
                VerifyRecordDTO.resourceCode,
                VerifyRecordDTO.action,
                VerifyRecordDTO.environmentTag,
                VerifyRecordDTO.verifyResult,
                now
            ).onDuplicateKeyUpdate()
                .set(VERIFY_RESULT, VerifyRecordDTO.verifyResult)
                .set(LAST_VERIFY_TIME, now)
                .execute()
        }
    }
}
