package com.tencent.devops.repository.sdk.github.request

import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.common.json.JsonIgnorePath
import com.tencent.devops.repository.sdk.github.GithubRequest
import com.tencent.devops.repository.sdk.github.response.GetTreeResponse
import org.apache.commons.lang3.StringUtils

data class GetTreeRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
    @JsonIgnorePath
    val treeSha: String,
    val recursive: String? = null
) : GithubRequest<GetTreeResponse>() {

    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/git/trees/$treeSha"
    } else {
        "repos/$repoName/git/trees/$treeSha"
    }
}
