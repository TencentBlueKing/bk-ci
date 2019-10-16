package com.tencent.devops.common.archive.shorturl.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ShortUrlResponse(
    @JsonProperty("result_code")
    val retCode: Int,
    @JsonProperty("short_url")
    val shortUrl: String
)