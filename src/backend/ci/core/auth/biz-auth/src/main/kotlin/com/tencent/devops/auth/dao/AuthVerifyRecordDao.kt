package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.model.auth.tables.TAuthTemporaryVerifyRecord
import com.tencent.devops.model.auth.tables.records.TAuthTemporaryVerifyRecordRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class AuthVerifyRecordDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        verifyRecordDTO: VerifyRecordDTO
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
                VERIFY_RESULT,
                LAST_VERIFY_TIME
            ).values(
                verifyRecordDTO.userId,
                verifyRecordDTO.projectId,
                verifyRecordDTO.resourceType,
                verifyRecordDTO.resourceCode,
                verifyRecordDTO.action,
                verifyRecordDTO.verifyResult,
                now
            ).onDuplicateKeyUpdate()
                .set(VERIFY_RESULT, verifyRecordDTO.verifyResult)
                .set(LAST_VERIFY_TIME, now)
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectCode: String,
        offset: Int,
        limit: Int
    ): Result<TAuthTemporaryVerifyRecordRecord> {
        with(TAuthTemporaryVerifyRecord.T_AUTH_TEMPORARY_VERIFY_RECORD) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(VERIFY_RESULT.eq(true))
                .orderBy(LAST_VERIFY_TIME.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun convert(record: TAuthTemporaryVerifyRecordRecord): VerifyRecordDTO {
        return with(record) {
            VerifyRecordDTO(
                userId = userId,
                projectId = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                verifyResult = verifyResult
            )
        }
    }
}
