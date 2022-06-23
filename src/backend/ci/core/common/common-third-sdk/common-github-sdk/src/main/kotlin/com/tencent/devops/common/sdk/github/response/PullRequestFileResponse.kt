package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonProperty

data class PullRequestFileResponse(
    val additions: Int,
    @JsonProperty("blob_url")
    val blobUrl: String,
    val changes: Int,
    @JsonProperty("contents_url")
    val contentsUrl: String,
    val deletions: Int,
    val filename: String,
    val patch: String,
    @JsonProperty("raw_url")
    val rawUrl: String,
    val sha: String,
    val status: String
)
