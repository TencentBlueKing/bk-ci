package com.tencent.bkrepo.nuget.pojo.v3.metadata.feed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Resource(
    @JsonProperty("@id")
    val id: String,
    @JsonProperty("@type")
    val type: String,
    val comment: String? = null,
    val clientVersion: String? = null
)
