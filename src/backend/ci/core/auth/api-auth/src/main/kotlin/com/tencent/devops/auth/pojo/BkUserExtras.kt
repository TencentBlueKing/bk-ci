package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户额外信息")
data class BkUserExtras(
    @Schema(description = "性别")
    val gender: String?,
    @Schema(description = "postName")
    @JsonProperty("postname")
    val postName: String?
)
