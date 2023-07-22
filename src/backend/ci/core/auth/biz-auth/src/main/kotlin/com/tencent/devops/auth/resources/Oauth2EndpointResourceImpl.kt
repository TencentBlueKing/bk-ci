package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.oauth2.Oauth2EndpointResource
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.Oauth2EndpointService
import com.tencent.devops.common.web.RestResource

@RestResource
class Oauth2EndpointResourceImpl constructor(
    oauth2EndpointService: Oauth2EndpointService
) : Oauth2EndpointResource {
    override fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String
    ): Result<String> {
        TODO("Not yet implemented")
    }

    override fun getAccessToken(
        userId: String,
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Result<Oauth2AccessTokenVo> {
        TODO("Not yet implemented")
    }
}
