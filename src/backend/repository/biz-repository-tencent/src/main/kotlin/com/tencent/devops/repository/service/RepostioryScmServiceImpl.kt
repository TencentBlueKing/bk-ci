package com.tencent.devops.repository.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.api.ServiceSvnResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepostioryScmServiceImpl @Autowired constructor(
    private val client: Client
):RepostioryScmService{
    override fun getProject(accessToken: String, userId: String):List<Project> {
        return client.get(ServiceGitResource::class).getProject(accessToken, userId).data ?: listOf()
    }

    override fun getAuthUrl(authParamJsonStr: String): String {
        return client.getScm(ServiceGitResource::class).getAuthUrl(authParamJsonStr).data ?: ""
    }

    override fun getToken(userId: String, code: String): GitToken {
        return client.get(ServiceGitResource::class).getToken(userId, code).data
                ?: throw RuntimeException("get token fail")
    }

    override fun getRedirectUrl(redirectUrlType: String): String {
        return client.get(ServiceGitResource::class).getRedirectUrl(redirectUrlType).data ?: ""
    }

    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        return client.get(ServiceGitResource::class).refreshToken(userId, accessToken).data!!
    }

    override fun getSvnFileContent(url: String, userId: String, svnType: String, filePath: String, reversion: Long, credential1: String, credential2: String?): String {
        return client.getScm(ServiceSvnResource::class).getFileContent(url, userId, svnType, filePath, reversion,
                credential1, credential2).data ?: ""
    }

    override fun getGitFileContent(repoName: String, filePath: String, authType: RepoAuthType?, token: String, ref: String): String {
        return client.getScm(ServiceGitResource::class).getGitFileContent(repoName!!, filePath.removePrefix("/"), authType, token, ref).data ?: ""
    }

    override fun getGitlabFileContent(repoUrl: String, repoName: String, filePath: String, ref: String, accessToken: String): String {
        return client.getScm(ServiceGitResource::class).getGitlabFileContent(
                repoUrl = repoUrl,
                repoName = repoName,
                filePath = filePath,
                ref = ref,
                accessToken = accessToken
        ).data ?: ""
    }
}