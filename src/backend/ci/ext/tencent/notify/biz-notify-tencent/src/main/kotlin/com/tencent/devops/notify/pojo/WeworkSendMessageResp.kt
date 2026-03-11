package com.tencent.devops.notify.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class WeworkSendMessageResp(
    @get:Schema(title = "errcode")
    @JsonProperty("errcode")
    val errCode: Int?,
    @get:Schema(title = "errmsg")
    @JsonProperty("errmsg")
    val errMsg: String?,
    @get:Schema(title = "invalidparty")
    @JsonProperty("invalidparty")
    val invalidParty: String?,
    @get:Schema(title = "invalidtag")
    @JsonProperty("invalidtag")
    val invalidTag: String?,
    @get:Schema(title = "invaliduser")
    @JsonProperty("invaliduser")
    val invalidUser: String?
)
