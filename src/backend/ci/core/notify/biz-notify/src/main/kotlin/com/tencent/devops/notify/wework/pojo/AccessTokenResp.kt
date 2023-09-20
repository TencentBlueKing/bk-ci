package com.tencent.devops.notify.wework.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class AccessTokenResp(
    @JsonProperty("access_token")
    val accessToken: String?,
    @JsonProperty("errcode")
    val errCode: Int?,
    @JsonProperty("errmsg")
    val errMsg: String?,
    @JsonProperty("expires_in")
    val expiresIn: Int?
) {
    fun isOk(): Boolean {
        return errCode == 0 && accessToken != null
    }
}
