package com.tencent.devops.repository.sdk.github.request

import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.common.json.JsonIgnorePath
import com.tencent.devops.repository.sdk.github.GithubRequest
import com.tencent.devops.repository.sdk.github.response.PullRequestResponse
import org.apache.commons.lang3.StringUtils

data class GetPullRequestRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
    @JsonIgnorePath
    val pullNumber: String
) : GithubRequest<PullRequestResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/pulls/$pullNumber"
    } else {
        "repos/$repoName/pulls/$pullNumber"
    }
}
