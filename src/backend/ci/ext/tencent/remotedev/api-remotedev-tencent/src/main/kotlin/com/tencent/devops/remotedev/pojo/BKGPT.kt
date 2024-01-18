package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "bk-chat")
data class BKGPT(
    @get:Schema(title = "插件透传")
    val data: Any,
    @get:Schema(title = "插件透传")
    val config: Any,
    @JsonProperty("bkdata_authentication_method")
    var method: String?,
    @JsonProperty("bkdata_data_token")
    var token: String?,
    @JsonProperty("bk_app_secret")
    var appSecret: String?,
    @JsonProperty("bk_app_code")
    var appCode: String?,
    @JsonProperty("bk_ticket")
    var ticket: String?
)
