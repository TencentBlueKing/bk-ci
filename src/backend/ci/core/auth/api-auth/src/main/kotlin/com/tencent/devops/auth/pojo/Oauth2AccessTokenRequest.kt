package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取token请求报文体")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "grantType",
    visible = true,
    defaultImpl = Oauth2AccessTokenRequest::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Oauth2AuthorizationCodeRequest::class, name = Oauth2AuthorizationCodeRequest.TYPE),
    JsonSubTypes.Type(value = Oauth2PassWordRequest::class, name = Oauth2PassWordRequest.TYPE),
    JsonSubTypes.Type(value = Oauth2RefreshTokenRequest::class, name = Oauth2RefreshTokenRequest.TYPE)
)
interface Oauth2AccessTokenRequest {
    @get:Schema(title = "授权类型", required = true)
    open val grantType: Oauth2GrantType
}
