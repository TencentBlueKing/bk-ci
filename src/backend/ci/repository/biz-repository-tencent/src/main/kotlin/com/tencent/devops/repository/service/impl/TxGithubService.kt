package com.tencent.devops.repository.service.impl

import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.client.Client
import com.tencent.devops.external.api.ExternalGithubResource
import com.tencent.devops.external.api.ServiceGithubResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.github.IGithubService
import com.tencent.devops.scm.pojo.Project
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxGithubService @Autowired constructor(
    private val client: Client,
    private val githubTokenService: GithubTokenService
) : IGithubService {

    override fun webhookCommit(event: String, guid: String, signature: String, body: String) {
        client.get(ExternalGithubResource::class).webhookCommit(
            event = event,
            guid = guid,
            signature = signature,
            body = body
        )
    }

    override fun addCheckRuns(token: String, projectName: String, checkRuns: GithubCheckRuns): GithubCheckRunsResponse {
        val result = client.get(ServiceGithubResource::class).addCheckRuns(
            accessToken = token,
            projectName = projectName,
            checkRuns = checkRuns
        )
        return result.data!!
    }

    override fun updateCheckRuns(token: String, projectName: String, checkRunId: Int, checkRuns: GithubCheckRuns) {
        client.get(ServiceGithubResource::class).updateCheckRuns(
            accessToken = token,
            projectName = projectName,
            checkRunId = checkRunId,
            checkRuns = checkRuns
        )
    }

    override fun getProject(projectId: String, userId: String, repoHashId: String?): AuthorizeResult {

        val accessToken = githubTokenService.getAccessToken(userId)
        if (accessToken == null) {
            val url = client.get(ServiceGithubResource::class).getOauth(
                projectId = projectId,
                userId = userId,
                repoHashId = repoHashId
            ).data?.redirectUrl ?: ""
            return AuthorizeResult(status = HTTP_403, url = url)
        }

        return try {
            val projects = client.get(ServiceGithubResource::class).getProject(
                accessToken = accessToken.accessToken,
                userId = userId
            ).data!!.map {
                Project(
                    id = it.id,
                    name = it.name,
                    nameWithNameSpace = it.fullName,
                    sshUrl = it.sshUrl,
                    httpUrl = it.httpUrl,
                    lastActivity = it.updatedAt
                )
            }
            AuthorizeResult(status = HTTP_200, url = "", project = projects.toMutableList())
        } catch (ignored: Throwable) {
            val url = client.get(ServiceGithubResource::class).getOauth(
                projectId = projectId,
                userId = userId,
                repoHashId = repoHashId
            ).data?.redirectUrl ?: ""
            AuthorizeResult(status = HTTP_403, url = url)
        }
    }

    override fun getBranch(token: String, projectName: String, branch: String?): GithubBranch? {
        return client.get(ServiceGithubResource::class).getGithubBranch(
            accessToken = token,
            projectName = projectName,
            branch = branch
        ).data
    }

    override fun getTag(token: String, projectName: String, tag: String): GithubTag? {
        return client.get(ServiceGithubResource::class)
            .getGithubTag(accessToken = token, projectName = projectName, tag = tag).data
    }

    override fun getFileContent(projectName: String, ref: String, filePath: String): String {
        return client.get(ServiceGithubResource::class).getFileContent(
            projectName = projectName,
            ref = ref,
            filePath = filePath
        ).data ?: ""
    }
}