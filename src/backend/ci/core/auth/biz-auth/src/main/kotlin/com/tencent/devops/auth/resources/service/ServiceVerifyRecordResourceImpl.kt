package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceVerifyRecordResource
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission

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

    override fun bathCreateOrUpdate(
        userId: String,
        projectCode: String,
        resourceType: String,
        permissionsResourcesMap: Map<AuthPermission, List<String>>
    ): Result<Boolean> {
        authVerifyRecordService.bathCreateOrUpdateVerifyRecord(
            permissionsResourcesMap = permissionsResourcesMap,
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType
        )
        return Result(true)
    }
}
