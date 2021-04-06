package com.tencent.bkrepo.nuget.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class Dependency(
    @JsonProperty("id")
    val packageId: String,
    val range: String,
    val registration: URI
)
