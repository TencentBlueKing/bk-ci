package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse
import com.tencent.devops.common.sdk.json.JsonIgnorePath
import org.apache.commons.lang3.StringUtils

data class GetCommitRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
    @JsonIgnorePath
    val ref: String,
    val page: Int = 1,
    @JsonProperty("per_page")
    val perPage: Int = 30
) : GithubRequest<CommitResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/commits/$ref"
    } else {
        "repos/$repoName/commits/$ref"
    }
}
