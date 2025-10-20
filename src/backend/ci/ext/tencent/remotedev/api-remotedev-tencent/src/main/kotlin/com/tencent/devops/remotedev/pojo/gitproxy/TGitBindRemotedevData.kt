package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitBindRemotedevData(
    val tgitId: Long,
    val tgitUrl: String,
    val projectIds: List<String>
)
