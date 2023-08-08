package com.tencent.devops.notify.wework.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class UploadMediaResp(
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("errcode")
    val errCode: Int?,
    @JsonProperty("errmsg")
    val errMsg: String?,
    @JsonProperty("media_id")
    val mediaId: String?,
    @JsonProperty("type")
    val type: String?
)
