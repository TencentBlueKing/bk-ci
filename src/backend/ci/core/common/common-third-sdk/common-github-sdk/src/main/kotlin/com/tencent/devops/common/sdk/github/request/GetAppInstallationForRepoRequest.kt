package com.tencent.devops.common.sdk.github.request

import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.GetAppInstallationResponse
import com.tencent.devops.common.sdk.json.JsonIgnorePath
import org.apache.commons.lang3.StringUtils

/**
 * Get a repository installation for the authenticated app
 * https://docs.github.com/en/rest/apps/apps#get-a-repository-installation-for-the-authenticated-app
 */
data class GetAppInstallationForRepoRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String
) : GithubRequest<GetAppInstallationResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/installation"
    } else {
        "repos/$repoName/installation"
    }
}
