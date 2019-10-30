package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.github.GithubRepository
import com.tencent.devops.repository.service.github.GithubService
import com.tencent.devops.repository.utils.Credential
import com.tencent.devops.repository.utils.CredentialUtils
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.api.ServiceSvnResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
        private val aesKey = "love_G/I%yP{?ST}2TXPg_love"
    }

    fun getFileContent(repositoryConfig: RepositoryConfig, filePath: String, reversion: String?, branch: String?, subModule: String? = null): String {
        val repo = repositoryService.serviceGet("", repositoryConfig)
        logger.info("get repo($repositoryConfig) file content in: $filePath (reversion:$reversion, branch:$branch)")
        return when (repo) {
            is CodeSvnRepository -> {
                logger.info("get file content of svn repo:\n$repo")
                if (reversion.isNullOrBlank()) throw RuntimeException("Illegal reversion: $reversion")
                getSvnSingleFile(
                        repo,
                        filePath.removePrefix("/"),
                        reversion!!.toLong()
                )
            }
            is CodeGitRepository -> {
                logger.info("get file content of git repo:\n$repo")
                if (!reversion.isNullOrBlank()) {
                    getGitSingleFile(
                            repo,
                            filePath,
                            reversion ?: "",
                            subModule
                    )
                } else {
                    getGitSingleFile(
                            repo,
                            filePath,
                            branch ?: "master",
                            subModule
                    )
                }
            }
            is CodeGitlabRepository -> {
                logger.info("get file content of gitlab repo:\n$repo")
                if (!reversion.isNullOrBlank()) {
                    getGitlabSingleFile(
                            repo,
                            filePath,
                            reversion ?: "",
                            subModule
                    )
                } else {
                    getGitlabSingleFile(
                            repo,
                            filePath,
                            branch ?: "master",
                            subModule
                    )
                }
            }
            is GithubRepository -> {
                logger.info("get file content of github repo:\n$repo")
                if (!reversion.isNullOrBlank()) {
                    getGithubFile(
                            repo,
                            filePath,
                            reversion!!,
                            subModule
                    )
                } else {
                    getGithubFile(
                            repo,
                            filePath,
                            branch ?: "master",
                            subModule
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
//            client.getScm(ServiceSvnResource::class).getFileContent(repo.url, repo.userName, svnType, filePath, reversion,
//                    credInfo.username, credInfo.privateKey).data ?: ""
            repositoryScmService.getSvnFileContent(repo.url, repo.userName, svnType, filePath, reversion,
                   credInfo.username, credInfo.privateKey)
        } else {
//            client.getScm(ServiceSvnResource::class).getSvnFileContent(repo.url, repo.userName, if (svnType.isBlank()) "SSH" else svnType, filePath, reversion,
//                    credInfo.privateKey, credInfo.passPhrase).data ?: ""
            repositoryScmService.getSvnFileContent(repo.url, repo.userName, if (svnType.isBlank()) "SSH" else svnType, filePath, reversion,
                   credInfo.privateKey, credInfo.passPhrase)
        }
    }

    private fun getGitSingleFile(repo: CodeGitRepository, filePath: String, ref: String, subModule: String?): String {
        val token = if (repo.authType == RepoAuthType.OAUTH) {
            AESUtil.decrypt(aesKey, gitTokenDao.getAccessToken(dslContext, repo.userName)?.accessToken ?: throw RuntimeException("get access token for user(${repo.userName}) fail"))
        } else {
            getCredential(repo.projectId ?: "", repo).privateKey
        }
        val projectName = if (!subModule.isNullOrBlank()) subModule else repo.projectName
        logger.info("getGitSingleFile for projectName: $projectName")
        return repositoryScmService.getGitFileContent(projectName!!, filePath.removePrefix("/"), repo.authType, token, ref)
//        return client.getScm(ServiceGitResource::class).getGitFileContent(projectName!!, filePath.removePrefix("/"), repo.authType, token, ref).data ?: ""
    }

    private fun getGitlabSingleFile(repo: CodeGitlabRepository, filePath: String, ref: String, subModule: String?): String {
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
//        return client.getScm(ServiceGitResource::class).getGitlabFileContent(
//                repoUrl = repo.url,
//                repoName = projectName ?: "",
//                filePath = filePath,
//                ref = ref,
//                accessToken = token
//        ).data ?: ""
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
        val credentialResult = client.get(ServiceCredentialResource::class).get(projectId, credentialId,
                encoder.encodeToString(pair.publicKey))
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error("Fail to get the credential($credentialId) of project($projectId) because of ${credentialResult.message}")
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!

        val privateKey = String(DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey))

        val passPhrase = if (credential.v2.isNullOrBlank()) "" else String(DHUtil.decrypt(
                decoder.decode(credential.v2),
                decoder.decode(credential.publicKey),
                pair.privateKey))

        val list = if (passPhrase.isBlank()) {
            listOf(privateKey)
        } else {
            listOf(privateKey, passPhrase)
        }
        return CredentialUtils.getCredential(repository, list, credential.credentialType)
    }
}