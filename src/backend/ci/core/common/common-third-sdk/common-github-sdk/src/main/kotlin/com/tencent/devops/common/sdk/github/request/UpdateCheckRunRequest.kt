package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.pojo.GithubAction
import com.tencent.devops.common.sdk.github.pojo.GithubOutput
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import com.tencent.devops.common.sdk.json.JsonIgnorePath
import org.apache.commons.lang3.StringUtils

data class UpdateCheckRunRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
    @JsonIgnorePath
    val checkRunId: String,
    val name: String? = null,
    @JsonProperty("details_url")
    val detailsUrl: String? = null,
    @JsonProperty("external_id")
    val externalId: String? = null,
    val status: String? = null,
    @JsonProperty("started_at")
    val startedAt: String? = null,
    val conclusion: String? = null,
    @JsonProperty("completed_at")
    val completedAt: String? = null,
    val output: GithubOutput? = null,
    val actions: List<GithubAction>? = null
) : GithubRequest<CheckRunResponse>() {
    override fun getHttpMethod() = HttpMethod.PUT

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/pulls/check-runs/$checkRunId"
    } else {
        "repos/$repoName/pulls/check-runs/$checkRunId"
    }
}
