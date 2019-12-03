package com.tencent.devops.worker.common.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkRepoFile(
    @JsonProperty("fullPath")
    val fullPath: String,
    @JsonProperty("size")
    val size: Long,
    @JsonProperty("folder")
    val folder: Boolean
)
