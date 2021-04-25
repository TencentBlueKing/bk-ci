package com.tencent.bkrepo.nuget.model.v3.search

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class SearchResponseDataVersion(
    @JsonProperty("@id")
    val id: URI,
    val version: String,
    val downloads: Int
)
