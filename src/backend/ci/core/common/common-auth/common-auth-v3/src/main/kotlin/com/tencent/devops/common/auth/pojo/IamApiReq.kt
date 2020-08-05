package com.tencent.devops.common.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class IamApiReq(
    val system: String,
    val type: String,
    val id: String,
    val name: String,
    val creator: String,
    val ancestors: List<AncestorsApiReq>? = emptyList(),
    @JsonProperty("bk_app_code")
	var bkAppCode: String,
    @JsonProperty("bk_app_code")
	var bkAppSecret: String,
    @JsonProperty("bk_app_code")
	var bkUsername: String,
    @JsonProperty("bk_token")
	val bk_token: String = ""
)