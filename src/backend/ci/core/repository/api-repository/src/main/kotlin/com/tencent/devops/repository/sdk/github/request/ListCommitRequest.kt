package com.tencent.devops.repository.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.common.json.JsonIgnorePath
import com.tencent.devops.repository.sdk.github.GithubRequest
import com.tencent.devops.repository.sdk.github.response.CommitResponse
import org.apache.commons.lang3.StringUtils

data class ListCommitRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
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

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/commits"
    } else {
        "repos/$repoName/commits"
    }
}
