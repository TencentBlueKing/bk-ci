package com.tencent.devops.remotedev.pojo.bkvision

import com.fasterxml.jackson.annotation.JsonProperty

data class QueryVariableDataBody(
    @JsonProperty("share_uid")
    val shareUid: String,
    val uid: String,
    val option: Any
)
