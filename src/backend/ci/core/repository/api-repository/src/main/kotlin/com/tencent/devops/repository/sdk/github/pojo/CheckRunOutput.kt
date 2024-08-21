package com.tencent.devops.repository.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Parameter

data class CheckRunOutput(
    @JsonProperty("annotations_count")
    val annotationsCount: Int? = 0,
    @JsonProperty("annotations_url")
    val annotationsUrl: String? = "",
    val summary: String?,
    var text: String?,
    val title: String?,
    @Parameter(description = "报表数据", required = true)
    val reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>? = Pair(listOf(), mutableMapOf())
)
