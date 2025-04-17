package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class UserSignatureStatusDTO(
    val user: String,
    @JsonProperty("is_whitelist_user")
    val whitelistUser: Boolean,
    val status: Int,
    @JsonProperty("scheme_qrcode_url")
    val schemeQrcodeUrl: String?
)
