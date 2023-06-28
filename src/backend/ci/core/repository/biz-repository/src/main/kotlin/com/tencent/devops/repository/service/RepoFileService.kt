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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode.NOT_AUTHORIZED_BY_OAUTH
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.github.IGithubService
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.repository.service.scm.Ip4Service
import com.tencent.devops.repository.utils.Credential
import com.tencent.devops.repository.utils.CredentialUtils
import com.tencent.devops.repository.utils.RepositoryUtils
import com.tencent.devops.scm.code.svn.ISvnService
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.ws.rs.NotFoundException

@Service
@Suppress("ALL")
class RepoFileService @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val gitTokenDao: GitTokenDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val githubService: IGithubService,
    private val gitService: IGitService,
    private val svnService: ISvnService,
    private val p4Service: Ip4Service
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
        subModule: String? = null,
        svnFullPath: Boolean = false
    ): String {
        val repo = repositoryService.serviceGet("", repositoryConfig)
        return getFileContent(
            repo = repo,
            filePath = filePath,
            reversion = reversion,
            branch = branch,
            subModule = subModule,
            svnFullPath = svnFullPath
        )
    }

    fun getFileContentByUrl(
        projectId: String,
        repoUrl: String,
        scmType: ScmType,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String? = null,
        svnFullPath: Boolean = false,
        credentialId: String
    ): String {
        logger.info("get repo($repoUrl) file content in: $filePath (reversion:$reversion, branch:$branch)")
        val repo = RepositoryUtils.buildRepository(
            projectId = projectId,
            userName = "",
            scmType = scmType,
            repositoryUrl = repoUrl,
            credentialId = credentialId
        )
        return getFileContent(
            repo = repo,
            filePath = filePath,
            reversion = reversion,
            branch = branch,
            subModule = subModule,
            svnFullPath = svnFullPath
        )
    }

    private fun getFileContent(
        repo: Repository,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String? = null,
        svnFullPath: Boolean = false
    ): String {
        logger.info("get repo(${repo.url}) file content in: $filePath (reversion:$reversion, branch:$branch)")
        return when (repo) {
            is CodeSvnRepository -> {
                logger.info("get file content of svn repo:\n$repo")
                if (reversion.isNullOrBlank()) throw ParamBlankException("Illegal reversion: $reversion")
                if (svnFullPath) {
                    getSvnSingleFileV2(
                        repo = repo,
                        filePath = filePath.removePrefix("/"),
                        reversion = reversion!!.toLong()
                    )
                } else {
                    getSvnSingleFile(
                        repo = repo,
                        filePath = filePath.removePrefix("/"),
                        reversion = reversion!!.toLong()
                    )
                }
            }
            is CodeGitRepository -> {
                logger.info("get file content of git repo:$repo")
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
                logger.info("get file content of gitlab repo: $repo")
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
                logger.info("get file content of github repo: $repo")
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
            is CodeTGitRepository -> {
                logger.info("get file content of tGit repo: $repo")
                if (!reversion.isNullOrBlank()) {
                    getTGitSingleFile(
                        repo = repo,
                        filePath = filePath,
                        ref = reversion ?: "",
                        subModule = subModule
                    )
                } else {
                    getTGitSingleFile(
                        repo = repo,
                        filePath = filePath,
                        ref = branch ?: "master",
                        subModule = subModule
                    )
                }
            }
            is CodeP4Repository -> {
                logger.info("get file content of tGit repo: $repo")
                getP4SingleFile(repo = repo, filePath = filePath, reversion = reversion!!)
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
            svnService.getFileContent(
                url = repo.url,
                userId = repo.userName,
                svnType = svnType,
                filePath = filePath,
                reversion = reversion,
                credential1 = credInfo.username,
                credential2 = credInfo.privateKey
            )
        } else {
            svnService.getFileContent(
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

    private fun getSvnSingleFileV2(repo: CodeSvnRepository, filePath: String, reversion: Long): String {
        val credInfo = getCredential(repo.projectId ?: "", repo)
        val svnType = repo.svnType?.toUpperCase() ?: "SSH"
        // Codecc传的filePath会带项目名,需要去掉
        val finalFilePath = SvnUtils.getSvnFilePath(url = repo.url, filePath = filePath)

        return if (svnType == "HTTP") {
            svnService.getFileContent(
                url = repo.url,
                userId = repo.userName,
                svnType = svnType,
                filePath = finalFilePath,
                reversion = reversion,
                credential1 = credInfo.username,
                credential2 = credInfo.privateKey
            )
        } else {
            svnService.getFileContent(
                url = repo.url,
                userId = repo.userName,
                svnType = if (svnType.isBlank()) "SSH" else svnType,
                filePath = finalFilePath,
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
                    ?: throw NotFoundException("get access token for user(${repo.userName}) fail")
            )
        } else {
            getCredential(repo.projectId ?: "", repo).privateKey
        }
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        logger.info("getGitSingleFile for projectName: $projectName")
        return gitService.getGitFileContent(
            repoUrl = repo.url,
            repoName = projectName!!,
            filePath = filePath.removePrefix("/"),
            authType = repo.authType,
            token = token,
            ref = ref
        )
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
        return gitService.getGitlabFileContent(
            repoUrl = repo.url,
            repoName = projectName ?: "",
            filePath = filePath,
            ref = ref,
            accessToken = token
        )
    }

    private fun getTGitSingleFile(
        repo: CodeTGitRepository,
        filePath: String,
        ref: String,
        subModule: String?
    ): String {
        logger.info("getTGitSingleFile for repo: ${repo.projectName}(subModule: $subModule)")
        val token = getCredential(repo.projectId ?: "", repo).privateKey
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        return gitService.getGitFileContent(
            repoUrl = repo.url,
            repoName = projectName ?: "",
            filePath = filePath,
            authType = RepoAuthType.HTTPS,
            token = token,
            ref = ref
        )
    }

    fun updateTGitFileContent(
        repositoryConfig: RepositoryConfig,
        userId: String,
        gitOperationFile: GitOperationFile
    ): Result<Boolean> {
        val repo = repositoryService.serviceGet("", repositoryConfig)
        return updateTGitSingleFile(
            repoUrl = repo.url,
            repoName = repo.projectName,
            token = getAndCheckOauthToken(userId).accessToken,
            gitOperationFile = GitOperationFile(
                filePath = gitOperationFile.filePath,
                branch = gitOperationFile.branch,
                encoding = gitOperationFile.encoding,
                content = gitOperationFile.content,
                commitMessage = gitOperationFile.commitMessage
            ),
            tokenType = TokenTypeEnum.OAUTH
        )
    }

    fun getAndCheckOauthToken(
        userId: String
    ): GitToken {
        return client.get(ServiceOauthResource::class).gitGet(userId).data ?: throw OauthForbiddenException(
            message = I18nUtil.getCodeLanMessage(NOT_AUTHORIZED_BY_OAUTH)
        )
    }

    private fun updateTGitSingleFile(
        repoUrl: String?,
        repoName: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.tGitUpdateFile(
            repoUrl = repoUrl,
            repoName = repoName,
            token = token,
            gitOperationFile = gitOperationFile,
            tokenType = tokenType
        )
    }

    private fun getGithubFile(repo: GithubRepository, filePath: String, ref: String, subModule: String?): String {
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        logger.info("getGithubFile for projectName: $projectName")
        return githubService.getFileContent(projectName!!, ref, filePath)
    }

    private fun getP4SingleFile(
        repo: CodeP4Repository,
        filePath: String,
        reversion: String
    ): String {
        val credInfo = getCredential(repo.projectId ?: "", repo)
        return p4Service.getFileContent(
            p4Port = repo.url,
            filePath = filePath,
            reversion = reversion.toInt(),
            username = credInfo.privateKey,
            password = credInfo.passPhrase!!
        )
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
        return CredentialUtils.getCredential(repository, list, credential.credentialType)
    }
}
