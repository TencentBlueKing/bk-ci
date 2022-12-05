package com.tencent.bkrepo.nuget.pojo.response.search

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class SearchResponseDataVersion(
    @JsonProperty("@id")
    val id: URI,
    val version: String,
    val downloads: Int
)
