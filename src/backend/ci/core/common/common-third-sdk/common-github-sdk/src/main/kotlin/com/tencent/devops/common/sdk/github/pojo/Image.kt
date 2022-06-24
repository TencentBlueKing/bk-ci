package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class Image(
    val alt: String,
    @JsonProperty("image_url")
    val imageUrl: String,
    val caption: String?
)
