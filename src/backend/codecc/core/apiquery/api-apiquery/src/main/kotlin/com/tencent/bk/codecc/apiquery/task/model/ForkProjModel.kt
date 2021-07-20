package com.tencent.bk.codecc.apiquery.task.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ForkProjModel(
    val path: String,
    @JsonProperty("path_with_namespace")
    val pathWithNameSpace: String,
    val name: String,
    val id: Int,
    @JsonProperty("name_with_namespace")
    val nameWithNameSpace: String
)