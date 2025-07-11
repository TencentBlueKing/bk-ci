/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.webhook.service.code

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.api.github.ServiceGithubPRResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.api.scm.ServiceTGitResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.sdk.github.request.GetCommitRequest
import com.tencent.devops.repository.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.repository.sdk.github.response.CommitResponse
import com.tencent.devops.repository.sdk.github.response.PullRequestResponse
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitCommitReviewInfo
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitTagInfo
import com.tencent.devops.scm.pojo.LoginSession
import com.tencent.devops.scm.pojo.RepoSessionRequest
import com.tencent.devops.scm.pojo.TapdWorkItem
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

@Suppress("ALL")
@Service
class GitScmService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitScmService::class.java)
    }

    fun getMergeRequestReviewersInfo(
        projectId: String,
        mrId: Long?,
        repo: Repository
    ): GitMrReviewInfo? {
        val type = getType(repo)
        if (mrId == null || type == null) return null

        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            if (type.first == RepoAuthType.OAUTH) {
                client.get(ServiceScmOauthResource::class).getMrReviewInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    mrId = mrId
                ).data
            } else {
                client.get(ServiceScmResource::class).getMrReviewInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    mrId = mrId
                ).data
            }
        } catch (e: Exception) {
            logger.warn("fail to get mr reviews info", e)
            null
        }
    }

    fun getMergeRequestInfo(
        projectId: String,
        mrId: Long?,
        repo: Repository
    ): GitMrInfo? {
        val type = getType(repo)
        if (mrId == null || type == null) return null

        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            if (type.first == RepoAuthType.OAUTH) {
                client.get(ServiceScmOauthResource::class).getMrInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    mrId = mrId
                ).data
            } else {
                client.get(ServiceScmResource::class).getMrInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    mrId = mrId
                ).data
            }
        } catch (e: Exception) {
            logger.warn("fail to get mr info", e)
            null
        }
    }

    fun getMergeRequestChangeInfo(
        projectId: String,
        mrId: Long?,
        repo: Repository
    ): GitMrChangeInfo? {
        val type = getType(repo)
        if (mrId == null || type == null) return null

        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            if (type.first == RepoAuthType.OAUTH) {
                client.get(ServiceScmOauthResource::class).getMergeRequestChangeInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    mrId = mrId
                ).data
            } else {
                client.get(ServiceScmResource::class).getMergeRequestChangeInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    mrId = mrId
                ).data
            }
        } catch (e: Exception) {
            logger.warn("fail to get mr info", e)
            null
        }
    }

    fun getChangeFileList(
        projectId: String,
        repo: Repository,
        from: String,
        to: String
    ): Set<String> {
        val type = getType(repo) ?: return emptySet()
        val changeSet = mutableSetOf<String>()
        try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            for (i in 1..10) {
                // 反向进行三点比较可以比较出rebase的真实提交
                val result = if (repo.getScmType() == ScmType.CODE_TGIT) {
                    client.get(ServiceTGitResource::class).getChangeFileList(
                        token = token,
                        tokenType = tokenType,
                        gitProjectId = repo.projectName,
                        from = from,
                        to = to,
                        straight = false,
                        page = i,
                        pageSize = 100,
                        url = repo.url
                    ).data
                } else {
                    client.get(ServiceGitResource::class).getChangeFileList(
                        token = token,
                        tokenType = tokenType,
                        gitProjectId = repo.projectName,
                        from = from,
                        to = to,
                        straight = false,
                        page = i,
                        pageSize = 100
                    ).data
                } ?: emptyList()
                changeSet.addAll(
                    result.map {
                        if (it.deletedFile) {
                            it.oldPath
                        } else {
                            it.newPath
                        }
                    }
                )
                if (result.size < 100) {
                    break
                }
            }
        } catch (ignore: Exception) {
            logger.warn("fail to get change file list", ignore)
        }
        return changeSet
    }

    fun getDefaultBranchLatestCommitInfo(
        projectId: String,
        repo: Repository
    ): Pair<String?, GitCommit?> {
        val type = getType(repo) ?: return Pair(null, null)
        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            val serviceGitResource = client.get(ServiceGitResource::class)
            val defaultBranch = serviceGitResource.getProjectInfo(
                token = token,
                tokenType = tokenType,
                gitProjectId = repo.projectName
            ).data?.defaultBranch ?: return Pair(null, null)
            val commitInfo = serviceGitResource.getRepoRecentCommitInfo(
                repoName = repo.projectName,
                sha = defaultBranch,
                token = token,
                tokenType = tokenType
            ).data
            Pair(defaultBranch, commitInfo)
        } catch (ignore: Exception) {
            logger.warn("fail to get default branch latest commit info", ignore)
            Pair(null, null)
        }
    }

    fun getWebhookCommitList(
        projectId: String,
        repo: Repository,
        mrId: Long?,
        page: Int,
        size: Int
    ): List<GitCommit> {
        val type = getType(repo) ?: return emptyList()
        if (mrId == null) return emptyList()
        val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
        val token = getToken(
            projectId = projectId,
            credentialId = repo.credentialId,
            userName = repo.userName,
            authType = tokenType,
            scmType = repo.getScmType(),
            repoUrl = repo.url
        )
        if (type.first == RepoAuthType.OAUTH) {
            return client.get(ServiceScmOauthResource::class).getMrCommitList(
                projectName = repo.projectName,
                url = repo.url,
                type = type.second,
                token = token,
                mrId = mrId,
                page = page,
                size = size
            ).data ?: emptyList()
        } else {
            return client.get(ServiceScmResource::class).getMrCommitList(
                projectName = repo.projectName,
                url = repo.url,
                type = type.second,
                token = token,
                mrId = mrId,
                page = page,
                size = size
            ).data ?: emptyList()
        }
    }

    fun getRepoAuthUser(
        projectId: String,
        repo: Repository
    ): String {
        val type = getType(repo) ?: return ""
        val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
        return try {
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            client.get(ServiceGitResource::class).getUserInfoByToken(
                token = token,
                tokenType = tokenType
            ).data?.username ?: ""
        } catch (ignore: Throwable) {
            logger.warn("fail to get repo auth user", ignore)
            ""
        }
    }

    private fun getToken(
        projectId: String,
        credentialId: String,
        userName: String,
        authType: TokenTypeEnum,
        scmType: ScmType,
        repoUrl: String = ""
    ): String {
        return if (authType == TokenTypeEnum.OAUTH) {
            client.get(ServiceOauthResource::class).gitGet(userName).data?.accessToken ?: ""
        } else {
            getCredential(projectId, credentialId, scmType = scmType, repoUrl = repoUrl)
        }
    }

    fun getCredential(
        projectId: String,
        credentialId: String,
        scmType: ScmType? = null,
        repoUrl: String = ""
    ): String {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId = projectId, credentialId = credentialId,
            publicKey = encoder.encodeToString(pair.publicKey)
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            throw ErrorCodeException(
                errorCode = credentialResult.status.toString(),
                defaultMessage = credentialResult.message
            )
        }

        val credential = credentialResult.data!!

        val privateKey = String(
            DHUtil.decrypt(
                data = decoder.decode(credential.v1),
                partBPublicKey = decoder.decode(credential.publicKey),
                partAPrivateKey = pair.privateKey
            )
        )
        if (credential.credentialType == CredentialType.USERNAME_PASSWORD &&
            (scmType == ScmType.CODE_GIT || scmType == ScmType.CODE_TGIT)
        ) {
            val password = String(
                DHUtil.decrypt(
                    data = decoder.decode(credential.v2),
                    partBPublicKey = decoder.decode(credential.publicKey),
                    partAPrivateKey = pair.privateKey
                )
            )
            return getSession(
                scmType = scmType,
                username = privateKey,
                password = password,
                url = repoUrl
            )?.privateToken ?: ""
        }
        return privateKey
    }

    private fun getType(repo: Repository): Pair<RepoAuthType?, ScmType>? {
        return when (repo) {
            is CodeGitRepository ->
                Pair(repo.authType, ScmType.CODE_GIT)
            is CodeTGitRepository ->
                Pair(repo.authType, ScmType.CODE_TGIT)
            is CodeGitlabRepository ->
                Pair(RepoAuthType.HTTP, ScmType.CODE_GITLAB)
            else ->
                return null
        }
    }

    /**
     * 获取Commit 评审信息
     */
    fun getCommitReviewInfo(
        projectId: String,
        commitReviewId: Long?,
        repo: Repository
    ): GitCommitReviewInfo? {
        val type = getType(repo)
        if (commitReviewId == null || type == null) return null

        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            if (type.first == RepoAuthType.OAUTH) {
                client.get(ServiceScmOauthResource::class).getCommitReviewInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    crId = commitReviewId
                ).data
            } else {
                client.get(ServiceScmResource::class).getCommitReviewInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    crId = commitReviewId
                ).data
            }
        } catch (e: Exception) {
            logger.warn("fail to get cr info", e)
            null
        }
    }

    fun getPrInfo(
        githubRepoName: String,
        pullNumber: String,
        repo: Repository
    ): PullRequestResponse? {
        return try {
            val accessToken = client.get(ServiceGithubResource::class).getAccessToken(
                userId = repo.userName
            ).data?.accessToken ?: ""
            val prInfo = client.get(ServiceGithubPRResource::class).getPullRequest(
                request = GetPullRequestRequest(
                    repoName = githubRepoName,
                    pullNumber = pullNumber
                ),
                token = accessToken
            ).data
            logger.info("get github pull info|repoName[$githubRepoName]|prNumber[$pullNumber]|[$prInfo]")
            prInfo
        } catch (ignored: Exception) {
            logger.warn("fail to get github pull request", ignored)
            null
        }
    }

    fun getSession(
        scmType: ScmType,
        username: String,
        password: String,
        url: String
    ): LoginSession? {
        return client.get(ServiceScmResource::class).getLoginSession(
            RepoSessionRequest(
                type = scmType,
                username = username,
                password = password,
                url = url
            )
        ).data
    }

    /**
     * 获取github commit 详情
     */
    fun getGithubCommitInfo(
        githubRepoName: String,
        commitId: String,
        repo: Repository
    ): CommitResponse? {
        return try {
            logger.info("get github commit info|repoName[$githubRepoName]|commit[$commitId]")
            val accessToken = client.get(ServiceGithubResource::class).getAccessToken(
                userId = repo.userName
            ).data?.accessToken ?: ""
            val commitInfo = client.get(ServiceGithubCommitsResource::class).getCommit(
                request = GetCommitRequest(
                    repoName = githubRepoName,
                    ref = commitId
                ),
                token = accessToken
            ).data
            commitInfo
        } catch (ignored: Exception) {
            logger.warn("fail to get github commit request", ignored)
            null
        }
    }

    fun getCredential(projectId: String, credentialId: String?): Pair<Boolean, String>? {
        if (credentialId.isNullOrBlank()) {
            return false to ""
        }
        return try {
            true to getCredential(
                projectId = projectId,
                credentialId = credentialId
            )
        } catch (ignored: Exception) {
            logger.warn("Fail to get credential: $credentialId", ignored)
            null
        }
    }

    fun getTag(
        projectId: String,
        repo: Repository,
        tagName: String
    ): GitTagInfo? {
        val type = getType(repo) ?: return null
        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            if (type.first == RepoAuthType.OAUTH) {
                client.get(ServiceScmOauthResource::class).getTagInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    tagName = tagName
                ).data
            } else {
                client.get(ServiceScmResource::class).getTagInfo(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    tagName = tagName
                ).data
            }
        } catch (e: Exception) {
            logger.warn("fail to get tag info", e)
            null
        }
    }

    fun getTapdItem(
        repo: Repository,
        projectId: String,
        refType: String,
        iid: Long
    ): List<TapdWorkItem> {
        val type = getType(repo) ?: return listOf()
        return try {
            val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType,
                scmType = repo.getScmType(),
                repoUrl = repo.url
            )
            if (type.first == RepoAuthType.OAUTH) {
                client.get(ServiceScmOauthResource::class).getTapdWorkItems(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    refType = refType,
                    iid = iid
                ).data
            } else {
                client.get(ServiceScmResource::class).getTapdWorkItems(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = type.second,
                    token = token,
                    refType = refType,
                    iid = iid
                ).data
            } ?: listOf()
        } catch (e: Exception) {
            logger.warn("fail to get tapd item", e)
            listOf()
        }
    }
}
