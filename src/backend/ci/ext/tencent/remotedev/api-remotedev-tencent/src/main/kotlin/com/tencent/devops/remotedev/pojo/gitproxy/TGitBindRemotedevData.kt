package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitBindRemotedevData(
    @field:JsonProperty("tGitId")
    val tgitId: Long,
    @field:JsonProperty("tGitUrl")
    val tgitUrl: String,
    val projectIds: List<String>
)
