package com.tencent.devops.external.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.external.api.ServiceGithubResource
import com.tencent.devops.external.pojo.GithubBranch
import com.tencent.devops.external.pojo.GithubCheckRuns
import com.tencent.devops.external.pojo.GithubCheckRunsResponse
import com.tencent.devops.external.pojo.GithubOauth
import com.tencent.devops.external.pojo.GithubRepository
import com.tencent.devops.external.pojo.GithubTag
import com.tencent.devops.external.service.github.GithubOauthService
import com.tencent.devops.external.service.github.GithubService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubResourceImpl @Autowired constructor(
    private val githubOauthService: GithubOauthService,
    private val githubService: GithubService
) : ServiceGithubResource {
    override fun getGithubBranch(accessToken: String, projectName: String, branch: String?): Result<GithubBranch?> {
        return Result(githubService.getBranch(accessToken, projectName, branch))
    }

    override fun getGithubTag(accessToken: String, projectName: String, tag: String): Result<GithubTag?> {
        return Result(githubService.getTag(accessToken, projectName, tag))
    }

    override fun getOauth(projectId: String, userId: String, repoHashId: String?): Result<GithubOauth> {
        return Result(githubOauthService.getGithubOauth(projectId, userId, repoHashId))
    }

    override fun getGithubAppUrl(): Result<String> {
        return Result(githubOauthService.getGithubAppUrl())
    }

    override fun getProject(accessToken: String, userId: String): Result<List<GithubRepository>> {
        return Result(githubService.getRepositories(accessToken))
    }

    override fun addCheckRuns(
        accessToken: String,
        projectName: String,
        checkRuns: GithubCheckRuns
    ): Result<GithubCheckRunsResponse> {
        return Result(githubService.addCheckRuns(accessToken, projectName, checkRuns))
    }

    override fun updateCheckRuns(
        accessToken: String,
        projectName: String,
        checkRunId: Int,
        checkRuns: GithubCheckRuns
    ): Result<Boolean> {
        githubService.updateCheckRuns(accessToken, projectName, checkRunId, checkRuns)
        return Result(true)
    }

    override fun getFileContent(projectName: String, ref: String, filePath: String): Result<String> {
        return Result(githubService.getFileContent(projectName, ref, filePath))
    }
}
