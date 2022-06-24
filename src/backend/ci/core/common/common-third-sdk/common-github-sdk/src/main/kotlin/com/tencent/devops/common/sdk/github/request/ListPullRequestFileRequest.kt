package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.PullRequestFileResponse

data class ListPullRequestFileRequest(
    @JsonIgnore
    val owner: String,
    @JsonIgnore
    val repo: String,
    @JsonIgnore
    val pullNumber: String,
    @JsonProperty("per_page")
    val perPage: Int = 30,
    val page: Int = 1
) : GithubRequest<List<PullRequestFileResponse>>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "/repos/$owner/$repo/pulls/$pullNumber/files"
}
