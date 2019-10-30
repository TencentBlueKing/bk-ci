package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.external.pojo.GithubCheckRuns
import com.tencent.devops.external.pojo.GithubCheckRunsResponse
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.utils.QualityUtils
import com.tencent.devops.scm.api.ServiceScmResource
import com.tencent.devops.process.utils.Credential
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.github.GithubRepository
import com.tencent.devops.scm.pojo.request.CommitCheckRequest
import com.tencent.devops.scm.api.ServiceScmOauthResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.Base64

@Service
class ScmService @Autowired constructor(private val client: Client) {
    private val logger = LoggerFactory.getLogger(ScmService::class.java)

    fun addGitCommitCheck(
        event: GitCommitCheckEvent,
        targetUrl: String,
        context: String,
        description: String
    ): String {
        with(event) {
            logger.info("Project($$projectId) add git commit($commitId) commit check.")

            checkRepoID(repositoryConfig)
            val repo = getRepo(projectId, repositoryConfig) as? CodeGitRepository ?: throw OperationException("不是Git 代码仓库")
            val isOauth = repo.credentialId.isEmpty()
            val token = if (isOauth) getAccessToken(repo.userName).first else
                getCredential(projectId, repo).privateKey

            val request = CommitCheckRequest(
                    repo.projectName,
                    repo.url,
                    ScmType.CODE_GIT,
                    null,
                    null,
                    token,
                    null,
                    commitId,
                    state,
                    targetUrl,
                    context,
                    description,
                    block,
                    event.mergeRequestId,
                    QualityUtils.getQualityGitMrResult(client, event)
            )
            if (isOauth) {
                client.getScm(ServiceScmOauthResource::class).addCommitCheck(request)
            } else {
                client.getScm(ServiceScmResource::class).addCommitCheck(request)
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
        val repo = getRepo(projectId, repositoryConfig) as? GithubRepository ?: throw OperationException("不是Github代码仓库")
        val accessToken = getGithubAccessToken(repo.userName)
        val checkRuns = GithubCheckRuns(
            name,
            commitId,
            detailUrl,
            externalId,
            status,
            startedAt,
            conclusion,
            completedAt
        )

        return client.get(com.tencent.devops.external.api.ServiceGithubResource::class).addCheckRuns(
            accessToken,
            repo.projectName,
            checkRuns
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
            name,
            commitId,
            detailUrl,
            externalId,
            status,
            startedAt,
            conclusion,
            completedAt
        )

        client.get(com.tencent.devops.external.api.ServiceGithubResource::class)
            .updateCheckRuns(accessToken, repo.projectName, checkRunId, checkRuns)
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

    private fun getRepo(projectId: String, repositoryConfig: RepositoryConfig, variables: Map<String, String>? = null): Repository {
        val repositoryId = if (variables == null || variables.isEmpty()) {
            repositoryConfig.getURLEncodeRepositoryId()
        } else {
            URLEncoder.encode(EnvUtils.parseEnv(repositoryConfig.getRepositoryId(), variables), "UTF-8")
        }
        logger.info("[$projectId] Start to get repo - ($repositoryId|${repositoryConfig.repositoryType})")
        val repoResult = client.get(ServiceRepositoryResource::class).get(projectId, repositoryId, repositoryConfig.repositoryType)
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
