package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.PullRequestResponse

data class GetPullRequestRequest(
    @JsonIgnore
    val owner: String,
    @JsonIgnore
    val repo: String,
    @JsonIgnore
    val pullNumber: String
) : GithubRequest<PullRequestResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "/repos/$owner/$repo/pulls/$pullNumber"
}
