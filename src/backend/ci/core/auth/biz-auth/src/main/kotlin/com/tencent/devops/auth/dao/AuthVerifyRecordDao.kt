package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.VerifyRecordInfo
import com.tencent.devops.model.auth.tables.TAuthTemporaryVerifyRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthVerifyRecordDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        verifyRecordInfo: VerifyRecordInfo
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
                verifyRecordInfo.userId,
                verifyRecordInfo.projectId,
                verifyRecordInfo.resourceType,
                verifyRecordInfo.resourceCode,
                verifyRecordInfo.action,
                verifyRecordInfo.environmentTag,
                verifyRecordInfo.verifyResult,
                now
            ).onDuplicateKeyUpdate()
                .set(VERIFY_RESULT, verifyRecordInfo.verifyResult)
                .set(LAST_VERIFY_TIME, now)
                .execute()
        }
    }
}
