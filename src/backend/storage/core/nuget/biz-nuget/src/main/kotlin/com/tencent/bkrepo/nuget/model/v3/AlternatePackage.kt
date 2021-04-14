package com.tencent.bkrepo.nuget.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class AlternatePackage(
    @JsonProperty("id")
    val packageId: String,
    val range: String?
)
