package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.oauth2.Oauth2ServiceEndpointResource
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.dto.Oauth2AuthorizationCodeDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.pojo.vo.Oauth2AuthorizationInfoVo
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.auth.service.oauth2.Oauth2EndpointService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class Oauth2ServiceEndpointResourceImpl(
    private val endpointService: Oauth2EndpointService,
    private val clientService: Oauth2ClientService,
) : Oauth2ServiceEndpointResource {
    override fun getAuthorizationInformation(
        userId: String,
        clientId: String,
        redirectUri: String
    ): Result<Oauth2AuthorizationInfoVo> {
        return Result(
            endpointService.getAuthorizationInformation(
                userId = userId,
                clientId = clientId,
                redirectUri = redirectUri
            )
        )
    }

    override fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String,
        authorizationCodeDTO: Oauth2AuthorizationCodeDTO
    ): Result<String> {
        return Result(
            endpointService.getAuthorizationCode(
                userId = userId,
                clientId = clientId,
                redirectUri = redirectUri,
                authorizationCodeDTO = authorizationCodeDTO
            )
        )
    }

    override fun getAccessToken(
        clientId: String,
        clientSecret: String,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Result<Oauth2AccessTokenVo> {
        return Result(
            endpointService.getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                accessTokenRequest = accessTokenRequest
            )
        )
    }

    override fun verifyAccessToken(
        clientId: String,
        clientSecret: String,
        accessToken: String
    ): Result<String> {
        return Result(
            endpointService.verifyAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                accessToken = accessToken
            )
        )
    }

    override fun createClientDetails(clientDetailsDTO: ClientDetailsDTO): Result<Boolean> {
        return Result(
            clientService.createClientDetails(
                clientDetailsDTO = clientDetailsDTO
            )
        )
    }

    override fun deleteClientDetails(clientId: String): Result<Boolean> {
        return Result(
            clientService.deleteClientDetails(clientId = clientId)
        )
    }
}
