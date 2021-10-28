package com.tencent.devops.notify.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class WeworkSendMessageResp(
    @ApiModelProperty(name = "errcode")
    @JsonProperty("errcode")
    val errCode: Int?,
    @ApiModelProperty(name = "errmsg")
    @JsonProperty("errmsg")
    val errMsg: String?,
    @ApiModelProperty(name = "invalidparty")
    @JsonProperty("invalidparty")
    val invalidParty: String?,
    @ApiModelProperty(name = "invalidtag")
    @JsonProperty("invalidtag")
    val invalidTag: String?,
    @ApiModelProperty(name = "invaliduser")
    @JsonProperty("invaliduser")
    val invalidUser: String?
)
