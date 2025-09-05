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

package com.tencent.devops.process.service.scm

import com.tencent.devops.common.api.constant.CommonMessageCode.GITLAB_INVALID
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_RETRY_3_FAILED
import com.tencent.devops.process.utils.Credential
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.ScmGitRepository
import com.tencent.devops.repository.pojo.ScmSvnRepository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.api.enums.ScmEventType
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.pojo.RepoSessionRequest
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import jakarta.ws.rs.NotFoundException

@Suppress("ALL")
@Service
class ScmProxyService @Autowired constructor(private val client: Client) {
    private val logger = LoggerFactory.getLogger(ScmProxyService::class.java)

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
                return getLatestRevision(
                    projectId = projectId,
                    repositoryConfig = repositoryConfig,
                    branchName = branchName,
                    additionalPath = null,
                    variables = variables
                )
            }

            override fun fail(e: Throwable): Result<RevisionInfo> {
                return Result(ERROR_RETRY_3_FAILED.toInt())
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
                return client.get(ServiceScmResource::class).getLatestRevision(
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
                    client.get(ServiceScmOauthResource::class).getLatestRevision(
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
                    val credInfo = getCredential(
                        projectId = projectId,
                        repository = repo,
                        getSession = true
                    )
                    client.get(ServiceScmResource::class).getLatestRevision(
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
            is CodeTGitRepository -> {
                val credInfo = getCredential(
                    projectId = projectId,
                    repository = repo,
                    getSession = true
                )
                return client.get(ServiceScmResource::class).getLatestRevision(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_TGIT,
                    branchName = branchName,
                    additionalPath = additionalPath,
                    privateKey = null,
                    passPhrase = null,
                    token = credInfo.privateKey,
                    region = null,
                    userName = credInfo.username
                )
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.get(ServiceScmResource::class).getLatestRevision(
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
                            branchName = githubBranch.name,
                            authorName = githubBranch.commit!!.commit?.author?.name ?: ""
                        )
                    )
                } else { // 否则查tag
                    val tagData =
                        client.get(clz).getGithubTag(
                            accessToken = accessToken,
                            projectName = repo.projectName,
                            tag = branchName!!
                        ).data ?: return Result(status = -1, message = "can not find tag $branchName")
                    return if (tagData.tagObject != null) {
                        Result(
                            RevisionInfo(
                                revision = tagData.tagObject!!.sha,
                                updatedMessage = "",
                                branchName = branchName,
                                authorName = ""
                            )
                        )
                    } else {
                        Result(status = -2, message = "can not find tag2 $branchName")
                    }
                }
            }
            is ScmGitRepository, is ScmSvnRepository -> {
                return client.get(ServiceScmRepositoryApiResource::class).getBranch(
                    projectId = projectId,
                    authRepository = AuthRepository(repo),
                    branch = branchName ?: ""
                ).data?.let {
                    Result(
                        RevisionInfo(
                            revision = it.sha,
                            updatedMessage = "",
                            branchName = it.name,
                            authorName = ""
                        )
                    )
                } ?: Result(status = -2, message = "can not find branch $branchName")
            }
            else -> {
                throw IllegalArgumentException("Unknown repo($repo)")
            }
        }
    }

    fun getDefaultBranch(
        projectId: String,
        repositoryConfig: RepositoryConfig
    ): String? {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig)
        return when (repo) {
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                val (token, tokenType) = if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    credInfo.first to TokenTypeEnum.OAUTH
                } else {
                    val credInfo = getCredential(projectId, repo)
                    credInfo.privateKey to TokenTypeEnum.PRIVATE_KEY
                }
                client.get(ServiceGitResource::class).getProjectInfo(
                    token = token,
                    tokenType = tokenType,
                    gitProjectId = repo.projectName
                ).data?.defaultBranch
            }

            is ScmGitRepository -> {
                val gitScmServerRepository = client.get(ServiceScmRepositoryApiResource::class).getServerRepository(
                    projectId = projectId,
                    authRepository = AuthRepository(repo)
                ).data as? GitScmServerRepository
                gitScmServerRepository?.defaultBranch
            }

            // SVN 仓库直接取关联仓库时的路径，部分用户可能没有根路径的访问权限
            // eg: http://svn.template.com/svn_group/svn_repo/trank/xxx 有权限
            //     http://svn.template.com/svn_group/svn_repo 无权限
            is CodeSvnRepository, is ScmSvnRepository -> {
                "/"
            }

            else -> {
                logger.warn("not support get default branch for ${repo.scmCode} repo[${repo.repoHashId}]|$projectId")
                null
            }
        }
    }

    fun listBranches(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        search: String? = null
    ): Result<List<String>> {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig)
        when (repo) {
            is CodeSvnRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.get(ServiceScmResource::class).listBranches(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_SVN,
                    privateKey = credInfo.privateKey,
                    passPhrase = credInfo.passPhrase,
                    token = null,
                    region = repo.region,
                    userName = credInfo.username,
                    search = search
                )
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.get(ServiceScmOauthResource::class).listBranches(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.first,
                        region = null,
                        userName = repo.userName,
                        search = search,
                        page = 1,
                        pageSize = 100
                    )
                } else {
                    val credInfo = getCredential(
                        projectId = projectId,
                        repository = repo,
                        getSession = true
                    )
                    client.get(ServiceScmResource::class).listBranches(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.privateKey,
                        region = null,
                        userName = credInfo.username,
                        search = search,
                        page = 1,
                        pageSize = 100
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.get(ServiceScmResource::class).listBranches(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_GITLAB,
                    privateKey = null,
                    passPhrase = null,
                    token = credInfo.privateKey,
                    region = null,
                    userName = credInfo.username,
                    search = search
                )
            }
            is GithubRepository -> {
                val token = getGithubAccessToken(repo.userName)
                return client.get(ServiceGithubResource::class).listBranches(
                    projectName = repo.projectName,
                    accessToken = token
                )
            }
            is CodeTGitRepository -> {
                return if (repo.authType == RepoAuthType.OAUTH) {
                    client.get(ServiceScmOauthResource::class).listBranches(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_TGIT,
                        privateKey = null,
                        passPhrase = null,
                        token = getTGitAccessToken(repo.userName),
                        region = null,
                        userName = repo.userName,
                        search = search,
                        page = 1,
                        pageSize = 100
                    )
                } else {
                    val credInfo = getCredential(
                        projectId = projectId,
                        repository = repo,
                        getSession = true
                    )
                    client.get(ServiceScmResource::class).listBranches(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_TGIT,
                        privateKey = null,
                        passPhrase = null,
                        token = credInfo.privateKey,
                        region = null,
                        userName = credInfo.username,
                        search = search,
                        page = 1,
                        pageSize = 100
                    )
                }
            }
            is ScmGitRepository, is ScmSvnRepository -> {
                return client.get(ServiceScmRepositoryApiResource::class).findBranches(
                    projectId = projectId,
                    authRepository = AuthRepository(repo),
                    search = search,
                    page = 1,
                    pageSize = 100
                ).let {
                    Result(it.data?.map { ref -> ref.name } ?: listOf())
                }
            }
            else -> {
                throw IllegalArgumentException("Unknown repo($repo)")
            }
        }
    }

    fun listTags(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        search: String? = null
    ): Result<List<String>> {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig)
        when (repo) {
            is CodeSvnRepository, is ScmSvnRepository -> {
                return Result(emptyList())
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.get(ServiceScmOauthResource::class).listTags(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        token = credInfo.first,
                        userName = repo.userName,
                        search = search
                    )
                } else {
                    val credInfo = getCredential(
                        projectId = projectId,
                        repository = repo,
                        getSession = true
                    )
                    client.get(ServiceScmResource::class).listTags(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_GIT,
                        token = credInfo.privateKey,
                        userName = credInfo.username,
                        search = search
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.get(ServiceScmResource::class).listTags(
                    projectName = repo.projectName,
                    url = repo.url,
                    type = ScmType.CODE_GITLAB,
                    token = credInfo.privateKey,
                    userName = credInfo.username,
                    search = search
                )
            }
            is GithubRepository -> {
                val token = getGithubAccessToken(repo.userName)
                return client.get(ServiceGithubResource::class).listTags(
                    projectName = repo.projectName,
                    accessToken = token
                )
            }
            is CodeTGitRepository -> {
                return if (repo.authType == RepoAuthType.OAUTH) {
                    client.get(ServiceScmOauthResource::class).listTags(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_TGIT,
                        token = getTGitAccessToken(repo.userName),
                        userName = repo.userName,
                        search = search
                    )
                } else {
                    val credInfo = getCredential(
                        projectId = projectId,
                        repository = repo,
                        getSession = true
                    )
                    client.get(ServiceScmResource::class).listTags(
                        projectName = repo.projectName,
                        url = repo.url,
                        type = ScmType.CODE_TGIT,
                        token = credInfo.privateKey,
                        userName = credInfo.username,
                        search = search
                    )
                }
            }
            is ScmGitRepository -> {
                return client.get(ServiceScmRepositoryApiResource::class).findTags(
                    projectId = projectId,
                    authRepository = AuthRepository(repo),
                    search = search,
                    page = 1,
                    pageSize = 100
                ).let {
                    Result(it.data?.map { ref -> ref.name } ?: listOf())
                }
            }
            else -> {
                throw IllegalArgumentException("Unknown repo($repo)")
            }
        }
    }

    fun addGitWebhook(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?
    ): CodeGitRepository {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeGitRepository
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.GIT_INVALID)
        val isOauth = repo.authType == RepoAuthType.OAUTH
        val token = if (isOauth) {
            getAccessToken(repo.userName).first
        } else {
            getCredential(
                projectId = projectId,
                repository = repo,
                getSession = true
            ).privateKey
        }
        val event = convertEvent(codeEventType)

        logger.info("Add git web hook event($event)")
        if (isOauth) {
            client.get(ServiceScmOauthResource::class).addWebHook(
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
            client.get(ServiceScmResource::class).addWebHook(
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

        return repo
    }

    fun addGitlabWebhook(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?
    ): Repository {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeGitlabRepository
            ?: throw ErrorCodeException(errorCode = GITLAB_INVALID)
        val token = getCredential(projectId, repo).privateKey
        client.get(ServiceScmResource::class).addWebHook(
            projectName = repo.projectName,
            url = repo.url,
            type = ScmType.CODE_GITLAB,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = repo.userName,
            event = convertEvent(codeEventType)
        )
        return repo
    }

    fun addSvnWebhook(projectId: String, repositoryConfig: RepositoryConfig): Repository {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeSvnRepository
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.SVN_INVALID)
        val credential = getCredential(
            projectId = projectId,
            repository = repo,
            getSession = true
        )
        val (isOauth, token) = getSvnToken(credential, repo.svnType, repo.userName)
        if (isOauth) {
            client.get(ServiceScmOauthResource::class).addWebHook(
                projectName = repo.projectName,
                url = repo.url,
                type = ScmType.CODE_SVN,
                privateKey = credential.username,
                passPhrase = credential.privateKey,
                token = token,
                region = repo.region,
                userName = credential.username,
                event = null
            )
        } else {
            client.get(ServiceScmResource::class).addWebHook(
                projectName = repo.projectName,
                url = repo.url,
                type = ScmType.CODE_SVN,
                privateKey = credential.username,
                passPhrase = credential.privateKey,
                token = token,
                region = repo.region,
                userName = credential.username,
                event = null
            )
        }
        return repo
    }

    fun addTGitWebhook(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?
    ): Repository {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeTGitRepository
            ?: throw ErrorCodeException(defaultMessage = "TGit", errorCode = ProcessMessageCode.TGIT_INVALID)

        if (repo.authType == RepoAuthType.OAUTH) {
            client.get(ServiceScmOauthResource::class).addWebHook(
                projectName = repo.projectName,
                url = repo.url,
                type = ScmType.CODE_TGIT,
                privateKey = null,
                passPhrase = null,
                token = getTGitAccessToken(repo.userName),
                region = null,
                userName = repo.userName,
                event = convertEvent(codeEventType)
            )
        } else {
            val credInfo = getCredential(
                projectId = projectId,
                repository = repo,
                getSession = true
            )
            client.get(ServiceScmResource::class).addWebHook(
                projectName = repo.projectName,
                url = repo.url,
                type = ScmType.CODE_TGIT,
                privateKey = null,
                passPhrase = null,
                token = credInfo.privateKey,
                region = null,
                userName = credInfo.username,
                event = convertEvent(codeEventType)
            )
        }
        return repo
    }

    fun addScmWebhook(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?
    ): Repository {
        checkRepoID(repositoryConfig)
        val repository = getRepo(projectId, repositoryConfig)
        val repo = repository as? ScmGitRepository
            ?: (repository as? ScmSvnRepository)
            ?: throw ErrorCodeException(
                defaultMessage = "ScmRepo",
                errorCode = ProcessMessageCode.SCM_REPO_INVALID
            )
        client.get(ServiceScmRepositoryApiResource::class).registerWebhook(
            projectId = projectId,
            eventType = (codeEventType?.convertScmEventType() ?: ScmEventType.PUSH).value,
            repository = repo
        )
        return repo
    }

    private fun convertEvent(codeEventType: CodeEventType?): String? {
        return when (codeEventType) {
            null, CodeEventType.PUSH -> CodeGitWebhookEvent.PUSH_EVENTS.value
            CodeEventType.TAG_PUSH -> CodeGitWebhookEvent.TAG_PUSH_EVENTS.value
            CodeEventType.MERGE_REQUEST, CodeEventType.MERGE_REQUEST_ACCEPT -> {
                CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value
            }
            CodeEventType.ISSUES -> CodeGitWebhookEvent.ISSUES_EVENTS.value
            CodeEventType.NOTE -> CodeGitWebhookEvent.NOTE_EVENTS.value
            CodeEventType.REVIEW -> CodeGitWebhookEvent.REVIEW_EVENTS.value
            else -> null
        }
    }

    fun addP4Webhook(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?
    ): Repository {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeP4Repository
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.P4_INVALID)
        val credential = getCredential(projectId, repo)
        client.get(ServiceScmResource::class).addWebHook(
            projectName = repo.projectName,
            url = repo.url,
            type = ScmType.CODE_P4,
            privateKey = null,
            passPhrase = credential.passPhrase,
            token = null,
            region = null,
            userName = credential.username,
            event = codeEventType?.name
        )
        return repo
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
        val repo = getRepo(projectId, repositoryConfig) as? GithubRepository
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.GITHUB_INVALID)
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
        checkRunId: Long,
        projectId: String,
        repositoryConfig: RepositoryConfig,
        name: String,
        commitId: String,
        detailUrl: String,
        externalId: String,
        status: String,
        startedAt: LocalDateTime?,
        conclusion: String?,
        completedAt: LocalDateTime?
    ) {
        logger.info("Project($projectId) update github commit($commitId) check runs")

        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? GithubRepository
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.GITHUB_INVALID)
        val accessToken = getGithubAccessToken(repo.userName)
        val checkRuns = GithubCheckRuns(
            name = name,
            headSha = commitId,
            detailsUrl = detailUrl,
            externalId = externalId,
            status = status,
            startedAt = startedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_INSTANT),
            conclusion = conclusion,
            completedAt = completedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_INSTANT)
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
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_REPO_ID_NULL)
            }
            RepositoryType.NAME -> if (repositoryConfig.repositoryName.isNullOrBlank()) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_REPO_NAME_NULL)
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
        val repoResult =
            client.get(ServiceRepositoryResource::class).get(projectId, repositoryId, repositoryConfig.repositoryType)
        if (repoResult.isNotOk() || repoResult.data == null) {
            logger.warn("$projectId|GET_REPO|$repositoryId|${repositoryConfig.repositoryType}|${repoResult.message}")
            throw ErrorCodeException(errorCode = repoResult.status.toString(), defaultMessage = repoResult.message)
        }
        return repoResult.data!!
    }

    private fun getCredential(
        projectId: String,
        repository: Repository,
        getSession: Boolean = false
    ): Credential {
        val credentialId = repository.credentialId
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId = projectId,
            credentialId = credentialId,
            publicKey = encoder.encodeToString(pair.publicKey),
            padding = true
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            throw ErrorCodeException(
                errorCode = credentialResult.status.toString(),
                defaultMessage = credentialResult.message
            )
        }

        val credential = credentialResult.data!!
        // 凭证字段定义: com.tencent.devops.ticket.pojo.enums.CredentialType
        val v1 = CredentialUtils.decode(
            encode = credential.v1,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        val v2 = CredentialUtils.decode(
            encode = credential.v2,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        val v3 = CredentialUtils.decode(
            encode = credential.v3,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        // 尝试获取token
        if (getSession && tryGetSession(repository, credential.credentialType)) {
            // USERNAME_PASSWORD v1 = username, v2 = password
            val session = try {
                client.get(ServiceScmResource::class).getLoginSession(
                    RepoSessionRequest(
                        type = repository.getScmType(),
                        username = v1,
                        password = v2,
                        url = repository.url
                    )
                ).data
            } catch (ignored: Exception) {
                logger.warn("fail to get login session", ignored)
                null
            }
            return Credential(
                username = v1,
                privateKey = session?.privateToken ?: "",
                passPhrase = v2,
                svnToken = session?.privateToken ?: ""
            )
        }
        // 按顺序封装凭证信息
        val list = when {
            v2.isBlank() -> listOf(v1)
            v3.isBlank() -> listOf(v1, v2)
            else -> listOf(v1, v2, v3)
        }
        return CredentialUtils.getCredential(repository, list, credential.credentialType).apply {
            this.credentialType = credential.credentialType
        }
    }

    private fun getAccessToken(userName: String): Pair<String, String?> {
        val gitOauthData = client.get(ServiceOauthResource::class).gitGet(userName).data
            ?: throw NotFoundException("cannot found oauth access token for user($userName)")
        return gitOauthData.accessToken to null
    }

    private fun getTGitAccessToken(userName: String): String {
        val gitOauthData = client.get(ServiceOauthResource::class).tGitGet(userName).data
            ?: throw NotFoundException("cannot found oauth access token for user($userName)")
        return gitOauthData.accessToken
    }

    private fun getGithubAccessToken(userName: String): String {
        val accessToken = client.get(ServiceGithubResource::class).getAccessToken(userName).data
            ?: throw NotFoundException("cannot find github oauth accessToekn for user($userName)")
        return accessToken.accessToken
    }

    private fun getSvnToken(credential: Credential, svnType: String?, userName: String) = when (svnType) {
        CodeSvnRepository.SVN_TYPE_SSH -> {
            // 凭证中存在token，则直接使用
            if (credential.credentialType == CredentialType.TOKEN_SSH_PRIVATEKEY) {
                Pair(false, credential.svnToken ?: "")
            } else {
                // 兜底，以当前代码关联人的oauthToken去操作
                try {
                    Pair(true, getAccessToken(userName).first)
                } catch (e: Exception) {
                    throw NotFoundException(
                        I18nUtil.getCodeLanMessage(
                            messageCode = ProcessMessageCode.ERROR_REPOSITORY_NOT_OAUTH,
                            params = arrayOf(userName)
                        )
                    )
                }
            }
        }
        CodeSvnRepository.SVN_TYPE_HTTP -> {
            // 凭证中存在token，则直接使用，反之用session接口返回值，此处svnToken是svn的token
            // 参考：1. com.tencent.devops.process.utils.CredentialUtils.getCredential
            //      2. com.tencent.devops.process.service.scm.ScmProxyService.getCredential
            Pair(false, credential.svnToken ?: "")
        }
        else -> {
            Pair(false, "")
        }
    }

    fun tryGetSession(repository: Repository, credentialType: CredentialType) =
        (repository is CodeGitRepository || repository is CodeTGitRepository || repository is CodeSvnRepository) &&
                (credentialType == CredentialType.USERNAME_PASSWORD)

    private fun CodeEventType.convertScmEventType() = when (this) {
        CodeEventType.PUSH -> ScmEventType.PUSH
        CodeEventType.PULL_REQUEST, CodeEventType.MERGE_REQUEST -> ScmEventType.PULL_REQUEST
        CodeEventType.TAG_PUSH -> ScmEventType.TAG
        CodeEventType.ISSUES -> ScmEventType.ISSUE
        CodeEventType.POST_COMMIT -> ScmEventType.POST_COMMIT
        else -> throw IllegalArgumentException("unknown code event type: $this")
    }
}
