package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.oauth2.Oauth2DesktopEndpointResource
import com.tencent.devops.auth.pojo.dto.Oauth2AuthorizationCodeDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AuthorizationInfoVo
import com.tencent.devops.auth.service.oauth2.Oauth2EndpointService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class Oauth2DesktopEndpointResourceImpl constructor(
    private val endpointService: Oauth2EndpointService
) : Oauth2DesktopEndpointResource {
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
}
