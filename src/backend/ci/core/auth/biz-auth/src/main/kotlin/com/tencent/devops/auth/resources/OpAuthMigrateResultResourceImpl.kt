package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.migrate.OpAuthMigrateResultResource
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

class OpAuthMigrateResultResourceImpl @Autowired constructor(
    private val permissionMigrateService: PermissionMigrateService
) : OpAuthMigrateResultResource {
    override fun fixMigrateCompareResult(
        verifyRecordDTO: VerifyRecordDTO
    ): Result<Boolean> {
        return Result(permissionMigrateService.fixMigrateCompareResult(verifyRecordDTO = verifyRecordDTO))
    }
}
