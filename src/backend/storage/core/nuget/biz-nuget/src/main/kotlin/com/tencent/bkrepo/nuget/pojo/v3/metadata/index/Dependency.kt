package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class Dependency(
    @JsonProperty("id")
    val packageId: String,
    val range: String? = null,
    val registration: URI? = null
)
