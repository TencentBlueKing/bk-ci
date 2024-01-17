package com.tencent.devops.notify.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class WeworkSendMessageResp(
    @Schema(title = "errcode")
    @JsonProperty("errcode")
    val errCode: Int?,
    @Schema(title = "errmsg")
    @JsonProperty("errmsg")
    val errMsg: String?,
    @Schema(title = "invalidparty")
    @JsonProperty("invalidparty")
    val invalidParty: String?,
    @Schema(title = "invalidtag")
    @JsonProperty("invalidtag")
    val invalidTag: String?,
    @Schema(title = "invaliduser")
    @JsonProperty("invaliduser")
    val invalidUser: String?
)
