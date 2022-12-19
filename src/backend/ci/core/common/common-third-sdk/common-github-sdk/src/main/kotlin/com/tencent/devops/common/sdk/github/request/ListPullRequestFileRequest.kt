package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.PullRequestFileResponse
import com.tencent.devops.common.sdk.json.JsonIgnorePath
import org.apache.commons.lang3.StringUtils

data class ListPullRequestFileRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
    @JsonIgnorePath
    val pullNumber: String,
    @JsonProperty("per_page")
    val perPage: Int = 30,
    val page: Int = 1
) : GithubRequest<List<PullRequestFileResponse>>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/pulls/$pullNumber/files"
    } else {
        "repos/$repoName/pulls/$pullNumber/files"
    }
}
