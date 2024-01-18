package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.archive.pojo.QueryData

@JsonIgnoreProperties(ignoreUnknown = true)
data class CgsQueryReq(
    val appName: String,
    val query: QueryData?,
    val page: Page?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryData(
    @JsonProperty("zoneId")
    val zoneId: String,
    @JsonProperty("machineType")
    val machineType: String,
    @JsonProperty("status")
    val status: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Page(
    @JsonProperty("start")
    val start: Int,
    @JsonProperty("limit")
    val limit: Int,
    @JsonProperty("sort")
    val sort: String? = ""
)
