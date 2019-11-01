package com.tencent.devops.plugin.worker.task

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.replaceCodeProp
import com.tencent.devops.plugin.worker.pojo.CoverityConfig
import com.tencent.devops.plugin.worker.task.codecc.util.CodeccUtils
import com.tencent.devops.plugin.worker.task.codecc.util.Coverity
import com.tencent.devops.plugin.worker.task.scm.util.RepositoryUtils
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.plugin.worker.api.CodeccSDKApi
import com.tencent.devops.worker.common.api.process.BuildSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.task.script.CommandFactory
import com.tencent.devops.worker.common.utils.CredentialUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLDecoder

/**
 * 构建脚本任务
 */
@TaskClassType(classTypes = [LinuxPaasCodeCCScriptElement.classType, LinuxCodeCCScriptElement.classType])
class LinuxCodeCCScriptTask : ITask() {

    private val api = ApiFactory.create(CodeccSDKApi::class)
    private val pipelineApi = ApiFactory.create(BuildSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val scriptType = taskParams["scriptType"] ?: ""
        val script = URLDecoder.decode(taskParams["script"] ?: "", "UTF-8")
        logger.info("Start to execute the script task($scriptType) ($script)")
        // 此处为了新引擎兼容，新引擎传递的参数是真实类型json，而不是单纯的String
        // 而CodeCC是用x,y,z这种方式对待List，所以在这强转并写入params中供CodeCC读取
        val languages = JsonUtil.to(taskParams["languages"]!!, object : TypeReference<List<String>>() {})

        val projectId = buildVariables.projectId
        val pipelineId = buildVariables.pipelineId
        val buildId = buildVariables.buildId

        // 如果指定_CODECC_FILTER_TOOLS，则只做_CODECC_FILTER_TOOLS的扫描
        val filterTools = buildVariables.variables["_CODECC_FILTER_TOOLS"] ?: ""
        val repos = getCodeccRepos(taskParams["id"] ?: "", projectId, pipelineId, buildId, buildVariables)
        val coverityConfig = CoverityConfig(
            name = taskParams["codeCCTaskName"] ?: "",
            cnName = taskParams["codeCCTaskCnName"] ?: "",
            projectType = CodeccUtils.projectType(languages),
            tools = JsonUtil.to(taskParams["tools"]!!, object : TypeReference<List<String>>() {}),
            asynchronous = taskParams["asynchronous"] == "true",
            filterTools = filterTools.split(",").map { it.trim() }.filter { it.isNotBlank() },
            repos = repos,
            scanCodePath = taskParams["path"] ?: "",
            scmType = repos.map { it.type }.first().toLowerCase(), // 每次扫描支持一种类型代码库，其他情况先不考虑
            certType = repos.map { it.authType }.first(), // 每次扫描支持一种类型代码库认证类型，其他情况先不考虑
            taskParams = taskParams,
            buildVariables = buildVariables,
            buildTask = buildTask
        )

        LoggerService.addNormalLine("buildVariables coverityConfig: $coverityConfig")

        // 先写入codecc任务
        try {
            api.saveTask(projectId, pipelineId, buildId)
        } catch (e: Exception) {
            LoggerService.addNormalLine("写入codecc任务失败: ${e.message}")
        }

        val cc = Coverity(coverityConfig)
        CommandFactory.create(scriptType, cc::coverity).execute(
            buildId,
            script,
            taskParams,
            buildVariables.variables,
            projectId,
            workspace,
            buildVariables.buildEnvs
        )
        ENV_FILES.forEach {
            addEnv(readScriptEnv(workspace, it))
        }
    }

    private fun getCodeccRepos(
        codeccId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildVariables: BuildVariables
    ): List<CoverityConfig.RepoItem> {

        val modelDetail = pipelineApi.getBuildDetail(projectId, pipelineId, buildId).data
            ?: throw RuntimeException("no model found in $buildId")
        val repoElementTypes = setOf(
            CodeSvnElement.classType,
            CodeGitElement.classType,
            CodeGitlabElement.classType,
            GithubElement.classType
        )
        val codeccElementTypes = setOf(LinuxCodeCCScriptElement.classType, LinuxPaasCodeCCScriptElement.classType)
        val repoItemList = mutableSetOf<CoverityConfig.RepoItem>()
        modelDetail.model.stages.forEach { stage ->
            stage.containers.forEach CONTAINER@{ container ->
                // 寻找codecc原子对应的container里面的代码库原子
                val isMatchContainer =
                    container.elements.any { it.getClassType() in codeccElementTypes && codeccId == it.id }
                if (!isMatchContainer) return@CONTAINER
                val items = container.elements.filter { it.getClassType() in repoElementTypes }.map {
                    when (it) {
                        is CodeSvnElement -> {
                            CoverityConfig.RepoItem(
                                replaceCodeProp(buildConfig(it), buildVariables.variables),
                                "SVN",
                                EnvUtils.parseEnv(it.path ?: "", buildVariables.variables),
                                EnvUtils.parseEnv(it.svnPath ?: "", buildVariables.variables)
                            )
                        }
                        is CodeGitElement -> CoverityConfig.RepoItem(
                            replaceCodeProp(buildConfig(it), buildVariables.variables),
                            "GIT",
                            EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        is CodeGitlabElement -> CoverityConfig.RepoItem(
                            replaceCodeProp(buildConfig(it), buildVariables.variables),
                            "GIT",
                            EnvUtils.parseEnv(it.path ?: "", buildVariables.variables)
                        )
                        is GithubElement -> CoverityConfig.RepoItem(
                            replaceCodeProp(buildConfig(it), buildVariables.variables),
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

    private fun readScriptEnv(workspace: File, file: String): Map<String, String> {
        val f = File(workspace, file)
        if (!f.exists()) {
            return mapOf()
        }
        if (f.isDirectory) {
            return mapOf()
        }

        val lines = f.readLines()
        if (lines.isEmpty()) {
            return mapOf()
        }
        // KEY-VALUE
        return lines.filter { it.contains("=") }.map {
            val split = it.split("=", ignoreCase = false, limit = 2)
            split[0].trim() to split[1].trim()
        }.toMap()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LinuxCodeCCScriptTask::class.java)
        private val ENV_FILES = arrayOf("result.log", "result.ini")
    }
}