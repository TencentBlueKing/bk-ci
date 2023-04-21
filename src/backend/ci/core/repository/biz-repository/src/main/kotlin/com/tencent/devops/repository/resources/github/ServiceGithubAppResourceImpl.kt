package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetAppInstallationForOrgRequest
import com.tencent.devops.common.sdk.github.request.GetAppInstallationForRepoRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.response.GetAppInstallationResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubAppResource
import com.tencent.devops.repository.github.config.GithubProperties
import com.tencent.devops.repository.github.service.GithubAppService
import com.tencent.devops.repository.github.service.GithubRepositoryService
import com.tencent.devops.repository.pojo.AppInstallationResult
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubAppResourceImpl @Autowired constructor(
    private val githubAppService: GithubAppService,
    private val githubProperties: GithubProperties,
    private val githubRepositoryService: GithubRepositoryService
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

    override fun isInstallApp(token: String, repoName: String): Result<AppInstallationResult> {
        val githubRepo = githubRepositoryService.getRepository(
            request = GetRepositoryRequest(repoName = repoName),
            token = token
        )
        // 先检查repo是否安装了app
        val repoInstallInfo =
            githubAppService.getAppInstallationForRepo(request = GetAppInstallationForRepoRequest(repoName = repoName))
        val appInstallResult = if (repoInstallInfo == null) {
            // 如果仓库没有安装，再检查组织是否安装app
            val orgInstallInfo = githubAppService.getAppInstallationForOrg(
                request = GetAppInstallationForOrgRequest(org = githubRepo.owner.login)
            )
            if (orgInstallInfo == null) {
                AppInstallationResult(
                    false,
                    "${githubProperties.serverUrl}/apps/${githubProperties.appName}/installations" +
                        "/new/permissions?target_id=${githubRepo.owner.id}"
                )
            } else {

                AppInstallationResult(
                    false,
                    "${githubProperties.serverUrl}/organizations/${githubRepo.owner.login}/" +
                        "settings/installations/${orgInstallInfo.id}"
                )
            }
        } else {
            AppInstallationResult(true)
        }
        return Result(appInstallResult)
    }
}
