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

package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.utils.QualityUtils
import com.tencent.devops.process.utils.Credential
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.Base64
import javax.ws.rs.NotFoundException

@Service
@Suppress("ALL")
class ScmCheckService @Autowired constructor(private val client: Client) {
    private val logger = LoggerFactory.getLogger(ScmCheckService::class.java)

    fun addGitCommitCheck(
        event: GitCommitCheckEvent,
        targetUrl: String,
        context: String,
        description: String,
        targetBranch: List<String>? = null
    ): String {
        with(event) {
            logger.info("Project($$projectId) add git commit($commitId) commit check.")

            checkRepoID(repositoryConfig)
            val repo = getRepo(projectId, repositoryConfig)
            val (isOauth, token, type) = when (repo) {
                is CodeGitRepository -> {
                    val isOauth = repo.credentialId.isEmpty()
                    val token = if (isOauth) {
                        getAccessToken(repo.userName).first
                    } else {
                        getCredential(projectId, repo).privateKey
                    }
                    Triple(isOauth, token, ScmType.CODE_GIT)
                }
                is CodeTGitRepository -> {
                    val isOauth = repo.credentialId.isEmpty()
                    val token = if (isOauth) {
                        getAccessToken(repo.userName).first
                    } else {
                        getCredential(projectId, repo).privateKey
                    }
                    Triple(isOauth, token, ScmType.CODE_TGIT)
                }
                else ->
                    throw OperationException("Not Git Code Repository")
            }
            logger.info("Project($projectId) add git commit($commitId) commit check for targetBranch($targetBranch)")
            val request = CommitCheckRequest(
                projectName = repo.projectName,
                url = repo.url,
                type = type,
                privateKey = null,
                passPhrase = null,
                token = token,
                region = null,
                commitId = commitId,
                state = state,
                targetUrl = targetUrl,
                context = context,
                description = description,
                block = block,
                mrRequestId = event.mergeRequestId,
                reportData = QualityUtils.getQualityGitMrResult(client, event),
                targetBranch = targetBranch
            )
            if (isOauth) {
                client.get(ServiceScmOauthResource::class).addCommitCheck(request)
            } else {
                client.get(ServiceScmResource::class).addCommitCheck(request)
            }
            return repo.projectName
        }
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
            ?: throw OperationException("Not GitHub Code Repository")
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
        startedAt: String?,
        conclusion: String?,
        completedAt: String?
    ) {
        logger.info("Project($projectId) update github commit($commitId) check runs")

        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? GithubRepository
            ?: throw OperationException("Not GitHub Code Repository")
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

    private fun getRepo(
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
        val repoResult = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repositoryId,
            repositoryType = repositoryConfig.repositoryType
        )
        if (repoResult.isNotOk() || repoResult.data == null) {
            logger.warn("getRepo|($repositoryConfig)|project($projectId)|${repoResult.message}")
            throw ErrorCodeException(
                errorCode = repoResult.status.toString(),
                defaultMessage = repoResult.message
            )
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
            logger.warn("getCredential|credential($credentialId)|project($projectId)|${credentialResult.message}")
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

        val passPhrase = if (credential.v2.isNullOrBlank()) "" else String(
            DHUtil.decrypt(
                data = decoder.decode(credential.v2),
                partBPublicKey = decoder.decode(credential.publicKey),
                partAPrivateKey = pair.privateKey
            )
        )

        val list = if (passPhrase.isBlank()) {
            listOf(privateKey)
        } else {
            listOf(privateKey, passPhrase)
        }

        return CredentialUtils.getCredential(
            repository = repository,
            credentials = list,
            credentialType = credentialResult.data!!.credentialType
        )
    }

    private fun getAccessToken(userName: String): Pair<String, String?> {
        val gitOauthData = client.get(ServiceOauthResource::class).gitGet(userName).data
            ?: throw NotFoundException("cannot found oauth access token for user($userName)")
        return gitOauthData.accessToken to null
    }

    private fun getGithubAccessToken(userName: String): String {
        val accessToken = client.get(ServiceGithubResource::class).getAccessToken(userName).data
            ?: throw NotFoundException("cannot find github oauth accessToekn for user($userName)")
        return accessToken.accessToken
    }
}
