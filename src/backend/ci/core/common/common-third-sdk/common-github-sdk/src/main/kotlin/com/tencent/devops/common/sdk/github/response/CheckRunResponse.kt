package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.pojo.CheckRunApp
import com.tencent.devops.common.sdk.github.pojo.CheckRunOutput
import com.tencent.devops.common.sdk.github.pojo.CheckSuite
import java.time.LocalDateTime

data class CheckRunResponse(
    val app: CheckRunApp,
    @JsonProperty("check_suite")
    val checkSuite: CheckSuite,
    @JsonProperty("completed_at")
    val completedAt: String,
    val conclusion: Any,
    @JsonProperty("details_url")
    val detailsUrl: String,
    @JsonProperty("external_id")
    val externalId: String,
    @JsonProperty("head_sha")
    val headSha: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    val id: Int,
    val name: String,
    @JsonProperty("node_id")
    val nodeId: String,
    val output: CheckRunOutput,
    @JsonProperty("pull_requests")
    val pullRequests: List<PullRequestResponse>,
    @JsonProperty("started_at")
    val startedAt: String,
    val status: String,
    val url: String
)