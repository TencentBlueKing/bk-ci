package com.tencent.bkrepo.nuget.pojo.v3.metadata.feed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Feed(
    val version: String,
    val resources: List<Resource>
)
