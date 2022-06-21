package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CommitFile(
    val filename: String,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val status: String,
    @JsonProperty("raw_url")
    val rawUrl: String,
    @JsonProperty("blob_url")
    val blobUrl: String,
    val patch: String
)
