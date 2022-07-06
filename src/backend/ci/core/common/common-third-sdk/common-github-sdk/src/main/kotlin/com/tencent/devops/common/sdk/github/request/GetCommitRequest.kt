package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse

data class GetCommitRequest(
    // val owner: String,
    // val repo: String,
    val id: Long,
    val ref: String,
    val page: Int = 1,
    @JsonProperty("per_page")
    val perPage: Int = 30
) : GithubRequest<CommitResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "repositories/$id/commits/$ref"
}
