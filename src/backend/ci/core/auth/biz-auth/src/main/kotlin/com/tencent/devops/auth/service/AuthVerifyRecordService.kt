package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthVerifyRecordDao
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthVerifyRecordService @Autowired constructor(
    val dslContext: DSLContext,
    val authVerifyRecordDao: AuthVerifyRecordDao
) {
    fun createOrUpdateVerifyRecord(verifyRecordDTO: VerifyRecordDTO) {
        authVerifyRecordDao.createOrUpdate(
            dslContext = dslContext,
            VerifyRecordDTO = verifyRecordDTO
        )
    }
}
