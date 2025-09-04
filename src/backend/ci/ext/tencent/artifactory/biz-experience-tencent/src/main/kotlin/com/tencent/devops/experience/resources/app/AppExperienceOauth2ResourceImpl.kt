package com.tencent.devops.experience.resources.app

import com.tencent.devops.auth.api.oauth2.Oauth2ServiceEndpointResource
import com.tencent.devops.auth.pojo.dto.Oauth2AuthorizationCodeDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AuthorizationInfoVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceOauth2Resource
import com.tencent.devops.experience.filter.annotions.AllowOuter

@RestResource
class AppExperienceOauth2ResourceImpl constructor(
    val client: Client
) : AppExperienceOauth2Resource {
    @AllowOuter
    override fun getAuthorizationInformation(
        userId: String,
        clientId: String,
        redirectUri: String
    ): Result<Oauth2AuthorizationInfoVo> {
        return client.get(Oauth2ServiceEndpointResource::class).getAuthorizationInformation(
            userId = userId,
            clientId = clientId,
            redirectUri = redirectUri
        )
    }

    @AllowOuter
    override fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String,
        authorizationCodeDTO: Oauth2AuthorizationCodeDTO
    ): Result<String> {
        return client.get(Oauth2ServiceEndpointResource::class).getAuthorizationCode(
            userId = userId,
            clientId = clientId,
            redirectUri = redirectUri,
            authorizationCodeDTO = authorizationCodeDTO
        )
    }
}
