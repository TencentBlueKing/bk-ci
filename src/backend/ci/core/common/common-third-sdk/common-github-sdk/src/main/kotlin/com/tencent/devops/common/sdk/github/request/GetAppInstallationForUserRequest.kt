package com.tencent.devops.common.sdk.github.request

import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.GetAppInstallationResponse

/**
 * Get a repository installation for the authenticated app
 * https://docs.github.com/en/rest/apps/apps#get-a-repository-installation-for-the-authenticated-app
 */
data class GetAppInstallationForUserRequest(
    // val owner: String,
    // val repo: String,
    val username: String
) : GithubRequest<GetAppInstallationResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "users/$username/installation"
}
