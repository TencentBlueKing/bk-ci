package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户额外信息")
data class BkUserExtras(
    @get:Schema(title = "性别")
    val gender: String?,
    @get:Schema(title = "postName")
    @JsonProperty("postname")
    val postName: String?,
    @get:Schema(title = "用户时区（IANA）")
    @JsonProperty("time_zone")
    val timeZone: String? = null
)
