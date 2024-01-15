package com.tencent.devops.notify.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class WeworkSendMessageResp(
    @Schema(description = "errcode")
    @JsonProperty("errcode")
    val errCode: Int?,
    @Schema(description = "errmsg")
    @JsonProperty("errmsg")
    val errMsg: String?,
    @Schema(description = "invalidparty")
    @JsonProperty("invalidparty")
    val invalidParty: String?,
    @Schema(description = "invalidtag")
    @JsonProperty("invalidtag")
    val invalidTag: String?,
    @Schema(description = "invaliduser")
    @JsonProperty("invaliduser")
    val invalidUser: String?
)
