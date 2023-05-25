/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
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
                authType = tokenType
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
                authType = tokenType
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
                authType = tokenType
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
                authType = tokenType
            )
            for (i in 1..10) {
                // 反向进行三点比较可以比较出rebase的真实提交
                val result = client.get(ServiceGitResource::class).getChangeFileList(
                    token = token,
                    tokenType = tokenType,
                    gitProjectId = repo.projectName,
                    from = from,
                    to = to,
                    straight = false,
                    page = i,
                    pageSize = 100
                ).data ?: emptyList()
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
                authType = tokenType
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
            authType = tokenType
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
                authType = tokenType
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

    private fun getToken(projectId: String, credentialId: String, userName: String, authType: TokenTypeEnum): String {
        return if (authType == TokenTypeEnum.OAUTH) {
            client.get(ServiceOauthResource::class).gitGet(userName).data?.accessToken ?: ""
        } else {
            getCredential(projectId, credentialId)
        }
    }

    fun getCredential(projectId: String, credentialId: String): String {
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

        return String(
            DHUtil.decrypt(
                data = decoder.decode(credential.v1),
                partBPublicKey = decoder.decode(credential.publicKey),
                partAPrivateKey = pair.privateKey
            )
        )
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
}
