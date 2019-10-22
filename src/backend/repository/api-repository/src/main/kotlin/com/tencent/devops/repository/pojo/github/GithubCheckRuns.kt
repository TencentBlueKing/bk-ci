package com.tencent.devops.external.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubCheckRuns(
    val name: String,
    @JsonProperty("head_sha")
    val headSha: String,
    @JsonProperty("details_url")
    val detailsUrl: String,
    @JsonProperty("external_id")
    val externalId: String,
    val status: String,
    @JsonProperty("started_at")
    val startedAt: String?,
    val conclusion: String?,
    @JsonProperty("completed_at")
    val completedAt: String?
)