package com.tencent.devops.notify.wework.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class SendMessageResp(
    @JsonProperty("errcode")
    val errCode: Int?,
    @JsonProperty("errmsg")
    val errMsg: String?,
    @JsonProperty("invalidparty")
    val invalidParty: String?,
    @JsonProperty("invalidtag")
    val invalidTag: String?,
    @JsonProperty("invaliduser")
    val invalidUser: String?
)
