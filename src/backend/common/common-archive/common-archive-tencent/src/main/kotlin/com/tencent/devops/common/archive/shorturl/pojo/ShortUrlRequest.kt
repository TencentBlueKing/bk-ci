package com.tencent.devops.common.archive.shorturl.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ShortUrlRequest(
    @JsonProperty("req_ver")
    val reqVer: Int = 1,
    @JsonProperty("app_id")
    val appid: Int,
    @JsonProperty("app_pass")
    val appPass: String,
    @JsonProperty("long_url")
    val longUrl: String,
    @JsonProperty("has_expire")
    val hasExpire: Int,
    @JsonProperty("uiExpireTime")
    val uiExpireTime: Long
)