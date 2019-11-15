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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.scm

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_RETRY_3_FAILED
import com.tencent.devops.process.utils.Credential
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.Base64

@Service
class ScmService @Autowired constructor(private val client: Client) {
    private val logger = LoggerFactory.getLogger(ScmService::class.java)

    fun recursiveFetchLatestRevision(
        projectId: String,
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        branchName: String?,
        variables: Map<String, String>,
        retry: Int = 1
    ): Result<RevisionInfo> {

        return RetryUtils.execute(object : RetryUtils.Action<Result<RevisionInfo>> {

            override fun execute(): Result<RevisionInfo> {
                return getLatestRevision(projectId, repositoryConfig, branchName, null, variables)
            }

            override fun fail(e: Throwable): Result<RevisionInfo> {
                AlertUtils.doAlert(
                    "SCM", AlertLevel.MEDIUM, "拉取最新版本号出现异常,重试${retry}次失败",
                    "拉取最新版本号出现异常, projectId: $projectId, pipelineId: $pipelineId $e"
                )
                return Result(ERROR_RETRY_3_FAILED)
            }
        }, retry, 2000)
    }

    fun getLatestRevision(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        branchName: String?,
        additionalPath: String?,
        variables: Map<String, String>?
    ): Result<RevisionInfo> {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig, variables)
        when (repo) {
            is CodeSvnRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).getLatestRevision(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_SVN,
                    branchName = branchName,
                    additionalPath = additionalPath,
                    privateKey = credInfo.privateKey,
                    passPhrase = credInfo.passPhrase,
                    token = null,
                    region = repo.region,
                    userName = credInfo.username
                )
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.getScm(ServiceScmOauthResource::class).getLatestRevision(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        branchName = branchName,
                        additionalPath = additionalPath,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.first,
                        region = null,
                        userName = repo.userName
                    )
                } else {
                    val credInfo = getCredential(projectId, repo)
                    client.getScm(ServiceScmResource::class).getLatestRevision(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        branchName = branchName,
                        additionalPath = additionalPath,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.privateKey,
                        region = null,
                        userName = credInfo.username
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).getLatestRevision(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_GITLAB,
                    branchName = branchName,
                    additionalPath = additionalPath,
                    privateKey = null,
                    passPhrase = null,
                    token = credInfo.privateKey,
                    region = null,
                    userName = credInfo.username
                )
            }
            is GithubRepository -> {
                val accessToken = getGithubAccessToken(repo.userName)

                val clz = ServiceGithubResource::class
                val githubBranch = client.get(clz).getGithubBranch(
                    accessToken = accessToken,
                    projectName = repo.projectName,
                    branch = branchName
                ).data
                if (githubBranch?.commit != null) { // 找到分支立即返回
                    return Result(
                        RevisionInfo(
                            revision = githubBranch.commit!!.sha,
                            updatedMessage = githubBranch.commit!!.commit?.message ?: "",
                            branchName = githubBranch.name
                        )
                    )
                } else { // 否则查tag
                    val tagData =
                        client.get(clz).getGithubTag(
                            accessToken = accessToken,
                            projectName = repo.projectName,
                            tag = branchName!!
                        ).data ?: return Result(-1, "can not find tag $branchName")
                    return if (tagData.tagObject != null) {
                        Result(
                            RevisionInfo(
                                revision = tagData.tagObject!!.sha,
                                updatedMessage = "",
                                branchName = branchName
                            )
                        )
                    } else {
                        Result(-2, "can not find tag2 $branchName")
                    }
                }
            }
            else -> {
                throw RuntimeException("Unknown repo($repo)")
            }
        }
    }

    fun listBranches(projectId: String, repositoryConfig: RepositoryConfig): Result<List<String>> {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig)
        when (repo) {
            is CodeSvnRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).listBranches(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_SVN,
                    privateKey = credInfo.privateKey,
                    passPhrase = credInfo.passPhrase,
                    token = null,
                    region = repo.region,
                    userName = credInfo.username
                )
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.getScm(ServiceScmOauthResource::class).listBranches(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.first,
                        region = null,
                        userName = repo.userName
                    )
                } else {
                    val credInfo = getCredential(projectId, repo)
                    client.getScm(ServiceScmResource::class).listBranches(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.privateKey,
                        region = null,
                        userName = credInfo.username
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).listBranches(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_GITLAB,
                    privateKey = null,
                    passPhrase = null,
                    token = credInfo.privateKey,
                    region = null,
                    userName = credInfo.username
                )
            }
            else -> {
                throw RuntimeException("Unknown repo($repo)")
            }
        }
    }

    fun listTags(projectId: String, repositoryConfig: RepositoryConfig): Result<List<String>> {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig)
        when (repo) {
            is CodeSvnRepository -> {
                throw OperationException("SVN 不支持获取Tag")
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.getScm(ServiceScmOauthResource::class).listTags(
                        projectName = repo.projectName, url = repo.url, type = ScmType.CODE_GIT,
                        token = credInfo.first, userName = repo.userName
                    )
                } else {
                    val credInfo = getCredential(projectId, repo)
                    client.getScm(ServiceScmResource::class).listTags(
                        projectName = repo.projectName, url = repo.url, type = ScmType.CODE_GIT,
                        token = credInfo.privateKey, userName = credInfo.username
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).listTags(
                    projectName = repo.projectName, url = repo.url, type = ScmType.CODE_GITLAB,
                    token = credInfo.privateKey, userName = credInfo.username
                )
            }
            else -> {
                throw RuntimeException("Unknown repo($repo)")
            }
        }
    }

    fun addGitWebhook(projectId: String, repositoryConfig: RepositoryConfig, codeEventType: CodeEventType?): String {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeGitRepository ?: throw OperationException("不是Git 代码仓库")
        val isOauth = repo.credentialId.isEmpty()
        val token = if (isOauth) getAccessToken(repo.userName).first else
            getCredential(projectId, repo).privateKey
        val event = when (codeEventType) {
            null, CodeEventType.PUSH -> CodeGitWebhookEvent.PUSH_EVENTS.value
            CodeEventType.TAG_PUSH -> CodeGitWebhookEvent.TAG_PUSH_EVENTS.value
            CodeEventType.MERGE_REQUEST, CodeEventType.MERGE_REQUEST_ACCEPT -> CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value
            else -> null
        }

        logger.info("Add git web hook event($event)")
        if (isOauth) {
            client.getScm(ServiceScmOauthResource::class).addWebHook(
                projectName = repo.projectName,
                url = repo.url,
                type = ScmType.CODE_GIT,
                privateKey = null,
                passPhrase = null,
                token = token,
                region = null,
                userName = repo.userName,
                event = event
            )
        } else {
            client.getScm(ServiceScmResource::class).addWebHook(
                projectName = repo.projectName,
                url = repo.url,
                type = ScmType.CODE_GIT,
                privateKey = null,
                passPhrase = null,
                token = token,
                region = null,
                userName = repo.userName,
                event = event
            )
        }

        return repo.projectName
    }

    fun addGitlabWebhook(projectId: String, repositoryConfig: RepositoryConfig): String {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeGitlabRepository
            ?: throw OperationException("不是Gitlab 代码仓库")
        val token = getCredential(projectId, repo).privateKey
        client.getScm(ServiceScmResource::class).addWebHook(
            projectName = repo.projectName,
            url = repo.url,
            type = ScmType.CODE_GITLAB,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = repo.userName,
            event = null
        )
        return repo.projectName
    }

    fun addSvnWebhook(projectId: String, repositoryConfig: RepositoryConfig): String {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeSvnRepository ?: throw OperationException("不是SVN 代码仓库")
        val credential = getCredential(projectId, repo)
        client.getScm(ServiceScmResource::class).addWebHook(
            projectName = repo.projectName,
            url = repo.url,
            type = ScmType.CODE_SVN,
            privateKey = credential.privateKey,
            passPhrase = credential.passPhrase,
            token = null,
            region = repo.region,
            userName = credential.username,
            event = null
        )
        return repo.projectName
    }

    fun addGithubCheckRuns(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        name: String,
        commitId: String,
        detailUrl: String,
        externalId: String,
        status: String,
        startedAt: String?,
        conclusion: String?,
        completedAt: String?
    ): GithubCheckRunsResponse {
        logger.info("Project($projectId) add github commit($commitId) check runs")

        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? GithubRepository ?: throw OperationException("不是Github代码仓库")
        val accessToken = getGithubAccessToken(repo.userName)
        val checkRuns = GithubCheckRuns(
            name = name,
            headSha = commitId,
            detailsUrl = detailUrl,
            externalId = externalId,
            status = status,
            startedAt = startedAt,
            conclusion = conclusion,
            completedAt = completedAt
        )

        return client.get(ServiceGithubResource::class).addCheckRuns(
            accessToken = accessToken,
            projectName = repo.projectName,
            checkRuns = checkRuns
        ).data!!
    }

    fun updateGithubCheckRuns(
        checkRunId: Int,
        projectId: String,
        repositoryConfig: RepositoryConfig,
        name: String,
        commitId: String,
        detailUrl: String,
        externalId: String,
        status: String,
        startedAt: String?,
        conclusion: String?,
        completedAt: String?
    ) {
        logger.info("Project($projectId) update github commit($commitId) check runs")

        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? GithubRepository ?: throw OperationException("不是Github代码仓库")
        val accessToken = getGithubAccessToken(repo.userName)
        val checkRuns = GithubCheckRuns(
            name = name,
            headSha = commitId,
            detailsUrl = detailUrl,
            externalId = externalId,
            status = status,
            startedAt = startedAt,
            conclusion = conclusion,
            completedAt = completedAt
        )

        client.get(ServiceGithubResource::class).updateCheckRuns(
            accessToken = accessToken,
            projectName = repo.projectName,
            checkRunId = checkRunId,
            checkRuns = checkRuns
        )
    }

    private fun checkRepoID(repositoryConfig: RepositoryConfig) {
        when (repositoryConfig.repositoryType) {
            RepositoryType.ID -> if (repositoryConfig.repositoryHashId.isNullOrBlank()) {
                throw ParamBlankException("Blank repository hash id")
            }
            RepositoryType.NAME -> if (repositoryConfig.repositoryName.isNullOrBlank()) {
                throw ParamBlankException("Blank repository name")
            }
        }
    }

    fun getRepo(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        variables: Map<String, String>? = null
    ): Repository {
        val repositoryId = if (variables == null || variables.isEmpty()) {
            repositoryConfig.getURLEncodeRepositoryId()
        } else {
            URLEncoder.encode(EnvUtils.parseEnv(repositoryConfig.getRepositoryId(), variables), "UTF-8")
        }
        logger.info("[$projectId] Start to get repo - ($repositoryId|${repositoryConfig.repositoryType})")
        val repoResult =
            client.get(ServiceRepositoryResource::class).get(projectId, repositoryId, repositoryConfig.repositoryType)
        if (repoResult.isNotOk() || repoResult.data == null) {
            logger.error("Fail to get the repo($repositoryConfig) of project($projectId) because of ${repoResult.message}")
            throw RuntimeException("Fail to get the repo")
        }
        return repoResult.data!!
    }

    private fun getCredential(projectId: String, repository: Repository): Credential {
        val credentialId = repository.credentialId
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId, credentialId,
            encoder.encodeToString(pair.publicKey)
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error("Fail to get the credential($credentialId) of project($projectId) because of ${credentialResult.message}")
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!

        val privateKey = String(
            DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )

        val passPhrase = if (credential.v2.isNullOrBlank()) "" else String(
            DHUtil.decrypt(
                decoder.decode(credential.v2),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )

        val list = if (passPhrase.isBlank()) {
            listOf(privateKey)
        } else {
            listOf(privateKey, passPhrase)
        }

        return CredentialUtils.getCredential(repository, list, credentialResult.data!!.credentialType)
    }

    private fun getAccessToken(userName: String): Pair<String, String?> {
        val gitOauthData = client.get(ServiceOauthResource::class).gitGet(userName).data
            ?: throw RuntimeException("cannot found oauth access token for user($userName)")
        return gitOauthData.accessToken to null
    }

    private fun getGithubAccessToken(userName: String): String {
        val accessToken = client.get(ServiceGithubResource::class).getAccessToken(userName).data
            ?: throw RuntimeException("cannot find github oauth accessToekn for user($userName)")
        return accessToken.accessToken
    }
}
