package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.plugin.worker.task.scm.util.RepositoryUtils
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.process.BuildSDKApi
import com.tencent.devops.worker.common.utils.CredentialUtils

object CodeccRepoHelper {

    private val pipelineApi = ApiFactory.create(BuildSDKApi::class)

    fun getCodeccRepos(
        codeccId: String,
        buildVariables: BuildVariables
    ): List<CodeccExecuteConfig.RepoItem> {
        val projectId = buildVariables.projectId
        val pipelineId = buildVariables.pipelineId
        val buildId = buildVariables.buildId

        val modelDetail = pipelineApi.getBuildDetail(projectId, pipelineId, buildId).data
            ?: throw RuntimeException("no model found in $buildId")
        val repoElementTypes = setOf(
            CodeSvnElement.classType,
            CodeGitElement.classType,
            CodeGitlabElement.classType,
            GithubElement.classType
        )
        val codeccElementTypes = setOf(LinuxCodeCCScriptElement.classType, LinuxPaasCodeCCScriptElement.classType)
        val repoItemList = mutableSetOf<CodeccExecuteConfig.RepoItem>()
        modelDetail.model.stages.forEach { stage ->
            stage.containers.forEach CONTAINER@{ container ->
                // 寻找codecc原子对应的container里面的代码库原子
                val isMatchContainer =
                    container.elements.any { it.getClassType() in codeccElementTypes && codeccId == it.id }
                if (!isMatchContainer) return@CONTAINER
                val items = container.elements.filter { it.getClassType() in repoElementTypes }.map {
                    when (it) {
                        is CodeSvnElement -> {
                            CodeccExecuteConfig.RepoItem(
                                RepositoryConfigUtils.replaceCodeProp(
                                    RepositoryConfigUtils.buildConfig(it),
                                    buildVariables.variables
                                ),
                                "SVN",
                                EnvUtils.parseEnv(it.path ?: "", buildVariables.variables),
                                EnvUtils.parseEnv(it.svnPath ?: "", buildVariables.variables)
                            )
                        }
                        is CodeGitElement -> CodeccExecuteConfig.RepoItem(
                            RepositoryConfigUtils.replaceCodeProp(
                                RepositoryConfigUtils.buildConfig(it),
                                buildVariables.variables
                            ),
                            "GIT",
                            EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        is CodeGitlabElement -> CodeccExecuteConfig.RepoItem(
                            RepositoryConfigUtils.replaceCodeProp(
                                RepositoryConfigUtils.buildConfig(it),
                                buildVariables.variables
                            ),
                            "GIT",
                            EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        is GithubElement -> CodeccExecuteConfig.RepoItem(
                            RepositoryConfigUtils.replaceCodeProp(
                                RepositoryConfigUtils.buildConfig(it),
                                buildVariables.variables
                            ),
                            "GIT",
                            EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        else -> throw OperationException("Unknown git element")
                    }
                }
                repoItemList.addAll(items)
            }
        }

        return repoItemList.map {
            val repo = RepositoryUtils.getRepository(it.repositoryConfig)
            val authType = when (repo) {
                is CodeGitRepository -> {
                    val authType = repo.authType?.name?.toUpperCase()
                    if (authType.isNullOrBlank()) "HTTP" else authType!!
                }
                is CodeSvnRepository -> {
                    val authType = repo.svnType?.toUpperCase()
                    if (authType.isNullOrBlank()) "SSH" else authType!!
                }
                is CodeGitlabRepository -> "HTTP"
                is GithubRepository -> "HTTP"
                else -> "SSH"
            }
            it.url = repo.url
            it.authType = authType
            it.repoHashId = repo.repoHashId ?: ""

            if (repo is CodeSvnRepository && authType == "HTTP") {
                val credentialsWithType = CredentialUtils.getCredentialWithType(repo.credentialId)
                val credentials = credentialsWithType.first
                val credentialType = credentialsWithType.second
                val svnCredential = SvnUtil.genSvnCredential(repo, credentials, credentialType)
                it.svnUerPassPair = Pair(svnCredential.username, svnCredential.password)
            }

            it
        }
    }

    fun getScmType(repos: List<CodeccExecuteConfig.RepoItem>): String {
        return repos.map { it.type }.first().toLowerCase() // 每次扫描支持一种类型代码库，其他情况先不考虑
    }

    fun getCertType(repos: List<CodeccExecuteConfig.RepoItem>): String {
        return repos.map { it.authType }.first() // 每次扫描支持一种类型代码库认证类型，其他情况先不考虑
    }
}