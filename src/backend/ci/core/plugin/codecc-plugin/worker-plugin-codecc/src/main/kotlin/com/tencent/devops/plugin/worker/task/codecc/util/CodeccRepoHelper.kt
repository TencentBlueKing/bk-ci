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

package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.InvalidParamException
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
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.process.pojo.task.PipelineBuildTaskInfo
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.process.BuildTaskSDKApi
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.worker.common.utils.CredentialUtils

object CodeccRepoHelper {

    private val pipelineTaskApi = ApiFactory.create(BuildTaskSDKApi::class)
    private val repoElementTypes = setOf(
        CodeSvnElement.classType,
        CodeGitElement.classType,
        CodeGitlabElement.classType,
        GithubElement.classType
    )
    private val codeccElementTypes = setOf(LinuxCodeCCScriptElement.classType, LinuxPaasCodeCCScriptElement.classType)

    fun getCodeccRepos(
        buildTask: BuildTask,
        buildVariables: BuildVariables
    ): List<CodeccExecuteConfig.RepoItem> {
        val buildTasks = pipelineTaskApi.getAllBuildTask().data
            ?: throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "get build task fail"
            )
        val codeccTask = buildTasks.first { it.taskType in codeccElementTypes }

        val repoItemList = mutableSetOf<CodeccExecuteConfig.RepoItem>()

        buildTasks.filter { it.containerId == codeccTask.containerId && it.taskType in repoElementTypes }.forEach {
            val item = when (it.taskType) {
                CodeSvnElement.classType -> {
                    CodeccExecuteConfig.RepoItem(
                        repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                        type = "SVN",
                        relPath = EnvUtils.parseEnv(it.getTaskParam("path"), buildVariables.variables),
                        relativePath = EnvUtils.parseEnv(it.getTaskParam("svnPath"), buildVariables.variables)
                    )
                }
                CodeGitElement.classType -> {
                    CodeccExecuteConfig.RepoItem(
                        repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                        type = "GIT",
                        relPath = EnvUtils.parseEnv(it.getTaskParam("path"), buildVariables.variables)
                    )
                }
                CodeGitlabElement.classType -> {
                    CodeccExecuteConfig.RepoItem(
                        repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                        type = "GIT",
                        relPath = EnvUtils.parseEnv(it.getTaskParam("path"), buildVariables.variables)
                    )
                }
                GithubElement.classType -> {
                    CodeccExecuteConfig.RepoItem(
                        repositoryConfig = replaceCodeProp(buildConfig(it), buildVariables.variables),
                        type = "GIT",
                        relPath = EnvUtils.parseEnv(it.getTaskParam("path"), buildVariables.variables)
                    )
                }
                else -> {
                    throw TaskExecuteException(
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                        errorMsg = "get codecc task fail"
                    )
                }
            }
            repoItemList.add(item)
        }

        // 新的拉代码插件接入模式
        val newRepoTaskIds = buildTask.buildVariable?.filter { it.key.startsWith("bk_repo_taskId_") }?.values
        newRepoTaskIds?.filter { taskId ->
            val containerId = buildTask.buildVariable!!["bk_repo_container_id_$taskId"]
            containerId.isNullOrBlank() || containerId == codeccTask.containerId
        }?.forEach { taskId ->
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

    private fun buildConfig(task: PipelineBuildTaskInfo): RepositoryConfig {
        return when (task.taskType) {
            CodeGitElement.classType, CodeSvnElement.classType, CodeGitlabElement.classType, GithubElement.classType ->
                RepositoryConfig(
                    task.getTaskParam("repositoryHashId"),
                    task.getTaskParam("repositoryName"),
                    RepositoryType.parseType(task.getTaskParam("repositoryType"))
                )
            else -> throw InvalidParamException("Unknown code element -> ${task.taskType}")
        }
    }
}
