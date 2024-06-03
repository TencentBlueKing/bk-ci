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

package com.tencent.devops.plugin.worker.task.scm

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ScmException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.SVNVersion
import com.tencent.devops.common.pipeline.enums.SvnDepth
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.plugin.worker.task.scm.git.CodeGitPullCodeSetting
import com.tencent.devops.plugin.worker.task.scm.git.GithubPullCodeSetting
import com.tencent.devops.plugin.worker.task.scm.git.GitlabPullCodeSetting
import com.tencent.devops.plugin.worker.task.scm.svn.CodeSvnPullCodeSetting
import com.tencent.devops.process.utils.PIPELINE_BUILD_SVN_REVISION
import com.tencent.devops.worker.common.constants.WorkerMessageCode.CODE_REPO_PARAM_NOT_IN_PARAMS
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import java.io.File

object SCM {

    @Suppress("ALL")
    fun getPullCodeSetting(
        scmType: ScmType,
        pipelineId: String,
        buildId: String,
        workspace: File,
        taskParams: Map<String, String>,
        variables: Map<String, String>
    ): IPullCodeSetting {

        return when (scmType) {
            ScmType.CODE_SVN -> {
                makeSvn(
                    pipelineId = pipelineId,
                    buildId = buildId,
                    workspace = workspace,
                    taskParams = taskParams,
                    variables = variables
                )
            }
            ScmType.CODE_GIT -> {
                makeGit(
                    pipelineId = pipelineId,
                    buildId = buildId,
                    workspace = workspace,
                    taskParams = taskParams,
                    variables = variables
                )
            }

            ScmType.CODE_GITLAB -> {
                makeGitlab(
                    pipelineId = pipelineId,
                    buildId = buildId,
                    workspace = workspace,
                    taskParams = taskParams,
                    variables = variables
                )
            }

            ScmType.GITHUB -> {
                makeGithub(
                    pipelineId = pipelineId,
                    buildId = buildId,
                    workspace = workspace,
                    taskParams = taskParams,
                    variables = variables
                )
            }
            else -> {
                throw ScmException("The scmType($scmType) is not implement", ScmType.GITHUB.name)
            }
        }
    }

    private fun codePullStrategy(taskParams: Map<String, String>): CodePullStrategy? {
        val strategyString = taskParams[CodeSvnElement.STRATEGY]
        return try { // 新的引擎传递的是和前端保持一样的大写，即枚举定义的名称
            if (strategyString == null) null else CodePullStrategy.valueOf(strategyString)
        } catch (ignored: Throwable) { // 出现异常是因为旧引擎用以下逻辑
            if (strategyString == null) null else CodePullStrategy.fromValue(strategyString)
        }
    }

    private fun repositoryConfig(taskParams: Map<String, String>, scmType: ScmType): RepositoryConfig {
        val repositoryType = RepositoryType.valueOf(taskParams[CodeSvnElement.REPO_TYPE] ?: RepositoryType.ID.name)
        val repositoryId = when (repositoryType) {
            RepositoryType.ID -> taskParams[CodeSvnElement.REPO_HASH_ID] ?: throw ScmException(
                MessageUtil.getMessageByLocale(
                    CODE_REPO_PARAM_NOT_IN_PARAMS,
                    AgentEnv.getLocaleLanguage(),
                    arrayOf("ID")
                ),
                scmType.name
            )
            RepositoryType.NAME -> taskParams[CodeSvnElement.REPO_NAME] ?: throw ScmException(
                MessageUtil.getMessageByLocale(
                    CODE_REPO_PARAM_NOT_IN_PARAMS,
                    AgentEnv.getLocaleLanguage(),
                    arrayOf("name")
                ),
                scmType.name
            )
        }
        return buildConfig(repositoryId, repositoryType)
    }

    private fun makeSvn(
        pipelineId: String,
        buildId: String,
        workspace: File,
        taskParams: Map<String, String>,
        variables: Map<String, String>
    ): CodeSvnPullCodeSetting {

        val enableSubmodule = taskParams[CodeSvnElement.enableSubmodule]?.toBoolean() ?: true

        return CodeSvnPullCodeSetting(
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig(taskParams, ScmType.CODE_SVN),
            branchName = taskParams[CodeSvnElement.BRANCH_NAME],
            revision = taskParams[CodeSvnElement.REVISION],
            strategy = codePullStrategy(taskParams),
            workspace = workspace,
            path = taskParams[CodeSvnElement.PATH],
            enableSubmodule = enableSubmodule,
            taskParams = variables,
            svnDepth = svnDepth(taskParams),
            svnPath = taskParams[CodeSvnElement.svnPath],
            specifyRevision = specifyRevision(taskParams),
            svnVersion = svnVersion(taskParams)
        )
    }

    private fun svnVersion(taskParams: Map<String, String>): SVNVersion? {
        val svnVersionStr = taskParams[CodeSvnElement.svnVersion]
        return if (BuildEnv.getBuildType() == BuildType.AGENT) {
            if (svnVersionStr.isNullOrBlank()) {
                null
            } else {
                SVNVersion.valueOf(svnVersionStr!!)
            }
        } else {
            null
        }
    }

    private fun specifyRevision(taskParams: Map<String, String>): String? {
        val revision = taskParams[CodeSvnElement.REVISION]
        val specifyRevisionRaw = taskParams[CodeSvnElement.specifyRevision]
        return if (specifyRevisionRaw.isNullOrBlank()) {
            null
        } else {
            if (specifyRevisionRaw == true.toString()) {
                revision ?: taskParams[PIPELINE_BUILD_SVN_REVISION]
            } else {
                null
            }
        }
    }

    private fun svnDepth(taskParams: Map<String, String>): SvnDepth {
        val svnDepth = taskParams[CodeSvnElement.svnDepth]

        return if (svnDepth.isNullOrBlank()) {
            SvnDepth.infinity
        } else {
            SvnDepth.valueOf(svnDepth!!)
        }
    }

    private fun makeGit(
        pipelineId: String,
        buildId: String,
        workspace: File,
        taskParams: Map<String, String>,
        variables: Map<String, String>
    ): CodeGitPullCodeSetting {

        return CodeGitPullCodeSetting(
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig(taskParams, ScmType.CODE_GIT),
            branchName = taskParams[CodeSvnElement.BRANCH_NAME],
            revision = taskParams[CodeSvnElement.REVISION],
            strategy = codePullStrategy(taskParams),
            workspace = workspace,
            path = taskParams[CodeSvnElement.PATH],
            enableSubmodule = taskParams[CodeSvnElement.enableSubmodule]?.toBoolean() ?: true,
            taskParams = variables,
            enableVirtualMergeBranch = taskParams[CodeSvnElement.enableVirtualMergeBranch]?.toBoolean() ?: false,
            modeType = taskParams[CodeGitElement.modeType],
            modeValue = taskParams[CodeGitElement.modeValue],
            gitType = ScmType.CODE_GIT
        )
    }

    private fun makeGitlab(
        pipelineId: String,
        buildId: String,
        workspace: File,
        taskParams: Map<String, String>,
        variables: Map<String, String>
    ): GitlabPullCodeSetting {

        return GitlabPullCodeSetting(
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig(taskParams, ScmType.CODE_GITLAB),
            branchName = taskParams[CodeSvnElement.BRANCH_NAME],
            revision = taskParams[CodeSvnElement.REVISION],
            strategy = codePullStrategy(taskParams),
            workspace = workspace,
            path = taskParams[CodeSvnElement.PATH],
            enableSubmodule = taskParams[CodeSvnElement.enableSubmodule]?.toBoolean() ?: true,
            taskParams = variables,
            enableVirtualMergeBranch = taskParams[CodeSvnElement.enableVirtualMergeBranch]?.toBoolean() ?: false,
            modeType = taskParams[CodeGitElement.modeType],
            modeValue = taskParams[CodeGitElement.modeValue],
            gitType = ScmType.CODE_GITLAB
        )
    }

    private fun makeGithub(
        pipelineId: String,
        buildId: String,
        workspace: File,
        taskParams: Map<String, String>,
        variables: Map<String, String>
    ): GithubPullCodeSetting {

        return GithubPullCodeSetting(
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig(taskParams, ScmType.GITHUB),
            strategy = codePullStrategy(taskParams),
            workspace = workspace,
            path = taskParams[CodeSvnElement.PATH],
            enableSubmodule = taskParams[CodeSvnElement.enableSubmodule]?.toBoolean() ?: true,
            taskParams = variables,
            enableVirtualMergeBranch = taskParams[CodeSvnElement.enableVirtualMergeBranch]?.toBoolean() ?: false,
            modeType = taskParams[CodeGitElement.modeType],
            modeValue = taskParams[CodeGitElement.modeValue],
            gitType = ScmType.GITHUB,
            revision = taskParams[CodeSvnElement.REVISION]
        )
    }
}
