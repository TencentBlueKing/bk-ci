package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceVerifyRecordResource
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceVerifyRecordResourceImpl @Autowired constructor(
    val authVerifyRecordService: AuthVerifyRecordService
) : ServiceVerifyRecordResource {
    override fun createOrUpdate(
        userId: String,
        verifyRecordDTO: VerifyRecordDTO
    ): Result<Boolean> {
        authVerifyRecordService.createOrUpdateVerifyRecord(
            verifyRecordDTO = verifyRecordDTO
        )
        return Result(true)
    }

    override fun delete(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        authVerifyRecordService.deleteVerifyRecord(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        return Result(true)
    }
}
