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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.service.github.GithubService
import com.tencent.devops.repository.utils.Credential
import com.tencent.devops.repository.utils.CredentialUtils
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class RepoFileService @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val gitTokenDao: GitTokenDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val githubService: GithubService,
    private val repositoryScmService: RepostioryScmService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepoFileService::class.java)
    }

    @Value("\${aes.git:#{null}}")
    private val aesKey: String = ""

    fun getFileContent(
        repositoryConfig: RepositoryConfig,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String? = null
    ): String {
        val repo = repositoryService.serviceGet("", repositoryConfig)
        logger.info("get repo($repositoryConfig) file content in: $filePath (reversion:$reversion, branch:$branch)")
        return when (repo) {
            is CodeSvnRepository -> {
                logger.info("get file content of svn repo:\n$repo")
                if (reversion.isNullOrBlank()) throw RuntimeException("Illegal reversion: $reversion")
                getSvnSingleFile(
                    repo = repo,
                    filePath = filePath.removePrefix("/"),
                    reversion = reversion!!.toLong()
                )
            }
            is CodeGitRepository -> {
                logger.info("get file content of git repo:\n$repo")
                if (!reversion.isNullOrBlank()) {
                    getGitSingleFile(
                        repo = repo,
                        filePath = filePath,
                        ref = reversion ?: "",
                        subModule = subModule
                    )
                } else {
                    getGitSingleFile(
                        repo = repo,
                        filePath = filePath,
                        ref = branch ?: "master",
                        subModule = subModule
                    )
                }
            }
            is CodeGitlabRepository -> {
                logger.info("get file content of gitlab repo:\n$repo")
                if (!reversion.isNullOrBlank()) {
                    getGitlabSingleFile(
                        repo = repo,
                        filePath = filePath,
                        ref = reversion ?: "",
                        subModule = subModule
                    )
                } else {
                    getGitlabSingleFile(
                        repo = repo,
                        filePath = filePath,
                        ref = branch ?: "master",
                        subModule = subModule
                    )
                }
            }
            is GithubRepository -> {
                logger.info("get file content of github repo:\n$repo")
                if (!reversion.isNullOrBlank()) {
                    getGithubFile(
                        repo = repo,
                        filePath = filePath,
                        ref = reversion!!,
                        subModule = subModule
                    )
                } else {
                    getGithubFile(
                        repo = repo,
                        filePath = filePath,
                        ref = branch ?: "master",
                        subModule = subModule
                    )
                }
            }
            else -> {
                "unsupported repo"
            }
        }
    }

    private fun getSvnSingleFile(repo: CodeSvnRepository, filePath: String, reversion: Long): String {
        val credInfo = getCredential(repo.projectId ?: "", repo)
        val svnType = repo.svnType?.toUpperCase() ?: "SSH"
        return if (svnType == "HTTP") {
            repositoryScmService.getSvnFileContent(
                url = repo.url, userId = repo.userName, svnType = svnType, filePath = filePath, reversion = reversion,
                credential1 = credInfo.username, credential2 = credInfo.privateKey
            )
        } else {
            repositoryScmService.getSvnFileContent(
                url = repo.url,
                userId = repo.userName,
                svnType = if (svnType.isBlank()) "SSH" else svnType,
                filePath = filePath,
                reversion = reversion,
                credential1 = credInfo.privateKey,
                credential2 = credInfo.passPhrase
            )
        }
    }

    private fun getGitSingleFile(repo: CodeGitRepository, filePath: String, ref: String, subModule: String?): String {
        val token = if (repo.authType == RepoAuthType.OAUTH) {
            AESUtil.decrypt(
                key = aesKey,
                content = gitTokenDao.getAccessToken(dslContext, repo.userName)?.accessToken
                    ?: throw RuntimeException("get access token for user(${repo.userName}) fail")
            )
        } else {
            getCredential(repo.projectId ?: "", repo).privateKey
        }
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        logger.info("getGitSingleFile for projectName: $projectName")
        return repositoryScmService.getGitFileContent(
            repoName = projectName!!,
            filePath = filePath.removePrefix("/"),
            authType = repo.authType,
            token = token,
            ref = ref
        )
//        return client.getScm(ServiceGitResource::class).getGitFileContent(projectName!!, filePath.removePrefix("/"), repo.authType, token, ref).data ?: ""
    }

    private fun getGitlabSingleFile(
        repo: CodeGitlabRepository,
        filePath: String,
        ref: String,
        subModule: String?
    ): String {
        logger.info("getGitlabSingleFile for repo: ${repo.projectName}(subModule: $subModule)")
        val token = getCredential(repo.projectId ?: "", repo).privateKey
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        return repositoryScmService.getGitlabFileContent(
            repoUrl = repo.url,
            repoName = projectName ?: "",
            filePath = filePath,
            ref = ref,
            accessToken = token
        )
    }

    private fun getGithubFile(repo: GithubRepository, filePath: String, ref: String, subModule: String?): String {
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        logger.info("getGithubFile for projectName: $projectName")
//        return client.get(ServiceGithubResource::class).getFileContent(projectName!!, ref, filePath).data ?: ""
        return githubService.getFileContent(projectName!!, ref, filePath)
    }

    private fun getCredential(projectId: String, repository: Repository): Credential {
        val credentialId = repository.credentialId
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId = projectId, credentialId = credentialId,
            publicKey = encoder.encodeToString(pair.publicKey)
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error("Fail to get the credential($credentialId) of project($projectId) because of ${credentialResult.message}")
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
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
        return CredentialUtils.getCredential(repository, list, credential.credentialType)
    }
}