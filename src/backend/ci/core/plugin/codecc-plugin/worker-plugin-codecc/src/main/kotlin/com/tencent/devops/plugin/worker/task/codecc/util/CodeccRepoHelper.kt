package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.replaceCodeProp
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.plugin.worker.task.scm.util.RepositoryUtils
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil
import com.tencent.devops.process.pojo.BuildTask
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
        buildTask: BuildTask,
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
                                repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                                type = "SVN",
                                relPath = EnvUtils.parseEnv(it.path ?: "", buildVariables.variables),
                                relativePath = EnvUtils.parseEnv(it.svnPath ?: "", buildVariables.variables)
                            )
                        }
                        is CodeGitElement -> CodeccExecuteConfig.RepoItem(
                            repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                            type = "GIT",
                            relPath = EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        is CodeGitlabElement -> CodeccExecuteConfig.RepoItem(
                            repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                            type = "GIT",
                            relPath = EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        is GithubElement -> CodeccExecuteConfig.RepoItem(
                            repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                            type = "GIT",
                            relPath = EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        else -> throw OperationException("Unknown git element")
                    }
                }
                repoItemList.addAll(items)
            }
        }

        val newRepoTaskIds = buildTask.buildVariable?.filter { it.key.startsWith("bk_repo_taskId_") }?.values
        newRepoTaskIds?.forEach { taskId ->
            val repoConfigType = buildTask.buildVariable!!["bk_repo_config_type_$taskId"]
            val repoType = buildTask.buildVariable!!["bk_repo_type_$taskId"]!!
            val localPath = buildTask.buildVariable!!["bk_repo_local_path_$taskId"] ?: ""
            val relativePath = buildTask.buildVariable!!["bk_repo_code_path_$taskId"] ?: ""

            val item = if (repoConfigType.isNullOrBlank()) {
                val url = buildTask.buildVariable!!["bk_repo_code_url_$taskId"]!!
                val authType = buildTask.buildVariable!!["bk_repo_auth_type_$taskId"]!!
                CodeccExecuteConfig.RepoItem(
                    repositoryConfig = null,
                    type = repoType,
                    relPath = localPath,
                    relativePath = relativePath,
                    url = url,
                    authType = authType
                )
            } else {
                val value = if (repoConfigType == RepositoryType.ID.name) {
                    buildTask.buildVariable!!["bk_repo_hashId_$taskId"]
                } else {
                    buildTask.buildVariable!!["bk_repo_name_$taskId"]
                }
                CodeccExecuteConfig.RepoItem(
                    repositoryConfig = buildConfig(value!!, RepositoryType.valueOf(repoConfigType!!)),
                    type = repoType,
                    relPath = localPath,
                    relativePath = relativePath
                )
            }
            repoItemList.add(item)
        }

        return repoItemList.map {
            if (it.repositoryConfig != null) {
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
                    val svnCredential = SvnUtil.getSvnCredential(repo, credentials, credentialType)
                    it.svnUerPassPair = Pair(svnCredential.username, svnCredential.password)
                }
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