package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthVerifyRecordDao
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.common.auth.api.AuthPermission
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
            verifyRecordDTO = verifyRecordDTO
        )
    }

    fun listByProjectCode(
        projectCode: String,
        offset: Int,
        limit: Int
    ): List<VerifyRecordDTO> {
        return authVerifyRecordDao.list(
            dslContext = dslContext,
            projectCode = projectCode,
            offset = offset,
            limit = limit
        ).map { authVerifyRecordDao.convert(it) }
    }

    fun bathCreateOrUpdateVerifyRecord(
        permissionsResourcesMap: Map<AuthPermission, List<String>>,
        userId: String,
        projectCode: String,
        resourceType: String
    ) {
        permissionsResourcesMap.forEach { (permission, resourceCodeList) ->
            resourceCodeList.forEach { resourceCode ->
                createOrUpdateVerifyRecord(
                    VerifyRecordDTO(
                        userId = userId,
                        projectId = projectCode,
                        resourceType = resourceType,
                        resourceCode = resourceCode,
                        action = "${resourceType}_${permission.value}",
                        verifyResult = true
                    )
                )
            }
        }
    }
}
