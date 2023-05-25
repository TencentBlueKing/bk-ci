package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.pojo.CheckRunApp
import com.tencent.devops.common.sdk.github.pojo.CheckRunOutput
import com.tencent.devops.common.sdk.github.pojo.CheckSuite

data class CheckRunResponse(
    val app: CheckRunApp,
    @JsonProperty("check_suite")
    val checkSuite: CheckSuite,
    @JsonProperty("completed_at")
    val completedAt: String?,
    val conclusion: Any?,
    @JsonProperty("details_url")
    val detailsUrl: String,
    @JsonProperty("external_id")
    val externalId: String,
    @JsonProperty("head_sha")
    val headSha: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    val id: Long,
    val name: String,
    @JsonProperty("node_id")
    val nodeId: String,
    val output: CheckRunOutput,
    @JsonProperty("pull_requests")
    val pullRequests: List<SamplePullRequestResponse>,
    @JsonProperty("started_at")
    val startedAt: String?,
    val status: String,
    val url: String
)

data class SamplePullRequestResponse(
    @JsonProperty("base")
    val base: SampleGithubCommitPointer,
    @JsonProperty("head")
    val head: SampleGithubCommitPointer,
    @JsonProperty("id")
    val id: Long, // 1008001512
    @JsonProperty("number")
    val number: Int, // 5
    @JsonProperty("url")
    val url: String // https://api.github.com/repos/ci-stream-test/test-3/pulls/5
)

data class SampleGithubCommitPointer(
    @JsonProperty("ref")
    val ref: String, // main
    @JsonProperty("repo")
    val repo: SampleRepository,
    @JsonProperty("sha")
    val sha: String // ecacb853ea7e42e80a9bda025d70e141e65ab1f0
)

data class SampleRepository(
    @JsonProperty("id")
    val id: Long, // 515188450
    @JsonProperty("name")
    val name: String, // test-3
    @JsonProperty("url")
    val url: String // https://api.github.com/repos/ci-stream-test/test-3
)
