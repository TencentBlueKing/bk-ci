package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.GithubTreeResponse

data class GetTreeRequest(
    @JsonIgnore
    val owner: String,
    @JsonIgnore
    val repo: String,
    @JsonIgnore
    val treeSha: String,
    val recursive: String? = null
) : GithubRequest<GithubTreeResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "/repos/$owner/$repo/git/trees/$treeSha"

}
