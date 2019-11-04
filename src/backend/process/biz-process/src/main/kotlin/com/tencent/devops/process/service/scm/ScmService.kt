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
import com.tencent.devops.external.pojo.GithubCheckRuns
import com.tencent.devops.external.pojo.GithubCheckRunsResponse
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_RETRY_3_FAILED
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.api.ServiceScmOauthResource
import com.tencent.devops.scm.api.ServiceScmResource
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.process.utils.Credential
import com.tencent.devops.repository.api.ServiceGithubResource
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
                    repo.projectName, repo.url, ScmType.CODE_SVN, branchName,
                    additionalPath, credInfo.privateKey, credInfo.passPhrase, null, repo.region, credInfo.username
                )
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.getScm(ServiceScmOauthResource::class).getLatestRevision(
                        repo.projectName, repo.url, ScmType.CODE_GIT, branchName,
                        additionalPath, null, null, credInfo.first, null, repo.userName
                    )
                } else {
                    val credInfo = getCredential(projectId, repo)
                    client.getScm(ServiceScmResource::class).getLatestRevision(
                        repo.projectName, repo.url, ScmType.CODE_GIT, branchName,
                        additionalPath, null, null, credInfo.privateKey, null, credInfo.username
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).getLatestRevision(
                    repo.projectName, repo.url, ScmType.CODE_GITLAB, branchName,
                    additionalPath, null, null, credInfo.privateKey, null, credInfo.username
                )
            }
            is GithubRepository -> {
                val accessToken = getGithubAccessToken(repo.userName)

                val clz = com.tencent.devops.external.api.ServiceGithubResource::class
                val githubBranch = client.get(clz).getGithubBranch(accessToken, repo.projectName, branchName).data
                if (githubBranch?.commit != null) { // 找到分支立即返回
                    return Result(
                        RevisionInfo(
                            githubBranch.commit!!.sha,
                            githubBranch.commit!!.commit?.message ?: "",
                            githubBranch.name
                        )
                    )
                } else { // 否则查tag
                    val tagData =
                        client.get(clz).getGithubTag(accessToken, repo.projectName, branchName!!).data
                            ?: return Result(-1, "can not find tag $branchName")
                    return if (tagData.tagObject != null) {
                        Result(
                            RevisionInfo(
                                tagData.tagObject!!.sha,
                                "",
                                branchName
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
                    repo.projectName, repo.url, ScmType.CODE_SVN,
                    credInfo.privateKey, credInfo.passPhrase, null, repo.region, credInfo.username
                )
            }
            is CodeGitRepository -> {
                val isOauth = repo.authType == RepoAuthType.OAUTH
                return if (isOauth) {
                    val credInfo = getAccessToken(repo.userName)
                    client.getScm(ServiceScmOauthResource::class).listBranches(
                        repo.projectName, repo.url, ScmType.CODE_GIT,
                        null, null, credInfo.first, null, repo.userName
                    )
                } else {
                    val credInfo = getCredential(projectId, repo)
                    client.getScm(ServiceScmResource::class).listBranches(
                        repo.projectName, repo.url, ScmType.CODE_GIT,
                        null, null, credInfo.privateKey, null, credInfo.username
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).listBranches(
                    repo.projectName, repo.url, ScmType.CODE_GITLAB,
                    null, null, credInfo.privateKey, null, credInfo.username
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
                        repo.projectName, repo.url, ScmType.CODE_GIT,
                        credInfo.first, repo.userName
                    )
                } else {
                    val credInfo = getCredential(projectId, repo)
                    client.getScm(ServiceScmResource::class).listTags(
                        repo.projectName, repo.url, ScmType.CODE_GIT,
                        credInfo.privateKey, credInfo.username
                    )
                }
            }
            is CodeGitlabRepository -> {
                val credInfo = getCredential(projectId, repo)
                return client.getScm(ServiceScmResource::class).listTags(
                    repo.projectName, repo.url, ScmType.CODE_GITLAB,
                    credInfo.privateKey, credInfo.username
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
        if (isOauth) client.getScm(ServiceScmOauthResource::class)
            .addWebHook(repo.projectName, repo.url, ScmType.CODE_GIT, null, null, token, null, repo.userName, event)
        else client.getScm(ServiceScmResource::class)
            .addWebHook(repo.projectName, repo.url, ScmType.CODE_GIT, null, null, token, null, repo.userName, event)

        return repo.projectName
    }

    fun addGitlabWebhook(projectId: String, repositoryConfig: RepositoryConfig): String {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeGitlabRepository
            ?: throw OperationException("不是Gitlab 代码仓库")
        val token = getCredential(projectId, repo).privateKey
        client.getScm(ServiceScmResource::class)
            .addWebHook(repo.projectName, repo.url, ScmType.CODE_GITLAB, null, null, token, null, repo.userName, null)
        return repo.projectName
    }

    fun addSvnWebhook(projectId: String, repositoryConfig: RepositoryConfig): String {
        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeSvnRepository ?: throw OperationException("不是SVN 代码仓库")
        val credential = getCredential(projectId, repo)
        client.getScm(ServiceScmResource::class)
            .addWebHook(
                repo.projectName,
                repo.url,
                ScmType.CODE_SVN,
                credential.privateKey,
                credential.passPhrase,
                null,
                repo.region,
                credential.username,
                null
            )
        return repo.projectName
    }

    fun addGitCommitCheck(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String,
        state: String,
        targetUrl: String,
        context: String,
        description: String,
        block: Boolean
    ): String {
        logger.info("Project($$projectId) add git commit($commitId) commit check.")

        checkRepoID(repositoryConfig)
        val repo = getRepo(projectId, repositoryConfig) as? CodeGitRepository ?: throw OperationException("不是Git 代码仓库")
        val isOauth = repo.credentialId.isEmpty()
        val token = if (isOauth) getAccessToken(repo.userName).first else
            getCredential(projectId, repo).privateKey

        if (isOauth) {
//            client.getScm(ServiceScmOauthResource::class)
//                .addCommitCheck(
//                    repo.projectName,
//                    repo.url,
//                    ScmType.CODE_GIT,
//                    null,
//                    null,
//                    token,
//                    null,
//                    commitId,
//                    state,
//                    targetUrl,
//                    context,
//                    description,
//                    block,
//                        null,
//                        ""
//                )
        } else {
//            client.getScm(ServiceScmResource::class)
//                .addCommitCheck(
//                    repo.projectName,
//                    repo.url,
//                    ScmType.CODE_GIT,
//                    null,
//                    null,
//                    token,
//                    null,
//                    commitId,
//                    state,
//                    targetUrl,
//                    context,
//                    description,
//                    block,
//                        null,
//                        ""
//                )
        }
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

    fun getRepo(projectId: String, repositoryConfig: RepositoryConfig, variables: Map<String, String>? = null): Repository {
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
