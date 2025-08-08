package com.tencent.devops.auth.pojo

import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "密码模式获取token请求报文体")
data class Oauth2PassWordRequest(
    @get:Schema(title = "授权类型", required = true)
    override val grantType: Oauth2GrantType,
    @get:Schema(title = "账号名称，用于密码模式", required = false)
    val userName: String? = null,
    @get:Schema(title = "密码，用于密码模式", required = false)
    val passWord: String? = null
) : Oauth2AccessTokenRequest {
    companion object {
        const val TYPE = "PASS_WORD"
    }
}
