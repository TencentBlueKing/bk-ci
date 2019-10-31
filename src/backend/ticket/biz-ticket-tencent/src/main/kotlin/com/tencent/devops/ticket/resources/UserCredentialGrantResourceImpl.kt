package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.UserCredentialGrantResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCredentialGrantResourceImpl @Autowired constructor(
    private val credentialService: CredentialService
) : UserCredentialGrantResource {
    override fun create(userId: String, projectId: String, credential: CredentialCreate): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credential.credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (credential.v1.isBlank()) {
            throw ParamBlankException("Invalid credential")
        }
        val authGroupList = mutableListOf<BkAuthGroup>()
        authGroupList.add(BkAuthGroup.MANAGER)
        authGroupList.add(BkAuthGroup.DEVELOPER)
        authGroupList.add(BkAuthGroup.TESTER)
        credentialService.userCreate(userId, projectId, credential, authGroupList)
        return Result(true)
    }
}