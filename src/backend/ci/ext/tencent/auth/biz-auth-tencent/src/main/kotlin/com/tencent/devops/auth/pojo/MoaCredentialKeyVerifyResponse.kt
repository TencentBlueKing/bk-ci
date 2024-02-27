package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "发送给kafka的云桌面信息")
data class MoaCredentialKeyVerifyResponse(
    @get:Schema(title = "请求返回标识")
    @JsonProperty("ReturnFlag")
    val returnFlag: Int,
    @get:Schema(title = "请求返回信息")
    @JsonProperty("msg")
    val msg: String?,
    @get:Schema(title = "用户名称")
    @JsonProperty("EnglishName")
    val userId: String?
)
