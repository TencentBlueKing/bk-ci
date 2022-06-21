package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse

data class ListCommitRequest(
    @JsonIgnore
    val owner: String,
    @JsonIgnore
    val repo: String,
    val sha: String,
    val path: String,
    val author: String,
    val since: String,
    val until: String,
    @JsonProperty("per_page")
    val perPage: Int,
    val page: Int
) : GithubRequest<List<CommitResponse>>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "repos/$owner/$repo/commits"
}