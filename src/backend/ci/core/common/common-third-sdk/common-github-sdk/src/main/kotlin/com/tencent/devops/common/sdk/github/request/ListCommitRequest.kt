package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse

data class ListCommitRequest(
    // val owner: String,
    // val repo: String,
    val id: Long,
    val sha: String? = null,
    val path: String? = null,
    val author: String? = null,
    val since: String? = null,
    val until: String? = null,
    @JsonProperty("per_page")
    val perPage: Int = 30,
    val page: Int = 1
) : GithubRequest<List<CommitResponse>>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "repositories/$id/commits"
}
