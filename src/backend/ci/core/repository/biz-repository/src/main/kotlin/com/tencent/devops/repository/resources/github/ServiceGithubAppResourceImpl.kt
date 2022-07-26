package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetAppInstallationForOrgRequest
import com.tencent.devops.common.sdk.github.request.GetAppInstallationForRepoRequest
import com.tencent.devops.common.sdk.github.response.GetAppInstallationResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubAppResource
import com.tencent.devops.repository.github.service.GithubAppService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubAppResourceImpl @Autowired constructor(
    private val githubAppService: GithubAppService
) : ServiceGithubAppResource {

    override fun getAppInstallationForRepo(
        request: GetAppInstallationForRepoRequest
    ): Result<GetAppInstallationResponse?> {
        return Result(githubAppService.getAppInstallationForRepo(request = request))
    }

    override fun getAppInstallationForOrg(
        request: GetAppInstallationForOrgRequest
    ): Result<GetAppInstallationResponse?> {
        return Result(githubAppService.getAppInstallationForOrg(request = request))
    }
}
