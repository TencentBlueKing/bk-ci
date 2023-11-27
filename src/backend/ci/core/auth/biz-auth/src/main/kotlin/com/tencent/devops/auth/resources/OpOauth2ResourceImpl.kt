package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.oauth2.OpOauth2Resource
import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.dto.ScopeOperationDTO
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.auth.service.oauth2.Oauth2ScopeOperationService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class OpOauth2ResourceImpl constructor(
    val clientService: Oauth2ClientService,
    val scopeOperationService: Oauth2ScopeOperationService
) : OpOauth2Resource {
    override fun createClientDetails(clientDetailsDTO: ClientDetailsDTO): Result<Boolean> {
        return Result(clientService.createClientDetails(clientDetailsDTO = clientDetailsDTO))
    }

    override fun deleteClientDetails(clientId: String): Result<Boolean> {
        return Result(clientService.deleteClientDetails(clientId = clientId))
    }

    override fun createScopeOperation(scopeOperationDTO: ScopeOperationDTO): Result<Boolean> {
        return Result(scopeOperationService.create(scopeOperationDTO = scopeOperationDTO))
    }

    override fun deleteScopeOperation(operationId: String): Result<Boolean> {
        return Result(scopeOperationService.delete(operationId = operationId))
    }
}
