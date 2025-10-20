package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitBindRemotedevData(
    @JsonProperty("tGitId")
    val tGitId: Long,
    @JsonProperty("tGitUrl")
    val tGitUrl: String,
    val projectIds: List<String>
)
