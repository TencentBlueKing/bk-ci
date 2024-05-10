package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitNamespace(
    val id: Long,
    val path: String,
    val kind: String,
    @JsonProperty("full_path")
    val fullPath: String?,
    @JsonProperty("parent_id")
    val parentId: Long?
)
