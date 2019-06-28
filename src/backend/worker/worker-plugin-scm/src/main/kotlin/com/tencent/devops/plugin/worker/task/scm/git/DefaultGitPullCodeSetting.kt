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

package com.tencent.devops.plugin.worker.task.scm.git

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.plugin.worker.task.scm.IPullCodeSetting
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_ALIASNAME
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_BRANCHNAME
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_URL
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.code.git.api.GitCredentialSetter
import java.io.File

abstract class DefaultGitPullCodeSetting(
    override val pipelineId: String,
    override val buildId: String,
    override val repositoryConfig: RepositoryConfig,
    override val branchName: String?,
    override val revision: String?,
    override val strategy: CodePullStrategy?,
    override val workspace: File,
    override val path: String?,
    override val enableSubmodule: Boolean,
    override val taskParams: Map<String, String>,
    open val enableVirtualMergeBranch: Boolean,
    open val modeType: String?,
    open val modeValue: String?,
    open val gitType: ScmType,
    open val convertSubmoduleUrl: Boolean
) : IPullCodeSetting {

    override fun getRemoteBranch(): String {
        if (modeType.isNullOrBlank() || modeValue.isNullOrBlank()) {
            return branchName ?: "master"
        }
        if (modeType == GitPullModeType.BRANCH.name) {
            return modeValue ?: "master"
        }
        return ""
    }

    fun pullGitCode(
        repo: Repository,
        workspace: File,
        credentialSetter: GitCredentialSetter
    ): MutableMap<String, String> {
        val task = when (strategy) {
            CodePullStrategy.FRESH_CHECKOUT -> freshCheckoutTask(
                url = repo.url,
                workspace = workspace,
                credentialSetter = credentialSetter,
                userName = repo.userName,
                projectName = repo.projectName
            )
            CodePullStrategy.REVERT_UPDATE -> revertCheckoutTask(
                url = repo.url,
                workspace = workspace,
                credentialSetter = credentialSetter,
                userName = repo.userName,
                projectName = repo.projectName
            )
            else -> updateTask(
                url = repo.url,
                workspace = workspace,
                credentialSetter = credentialSetter,
                userName = repo.userName,
                projectName = repo.projectName
            )
        }
        val env = mutableMapOf<String, String>()
        env["$PIPELINE_MATERIAL_URL.${repositoryConfig.getRepositoryId()}"] = repo.url
        env["$PIPELINE_MATERIAL_BRANCHNAME.${repositoryConfig.getRepositoryId()}"] = getRemoteBranch()
        env["$PIPELINE_MATERIAL_ALIASNAME.${repositoryConfig.getRepositoryId()}"] = repo.aliasName
        val performEnv = task.perform()
        if (null != performEnv) {
            env.putAll(performEnv)
        }
        return env
    }

    private fun freshCheckoutTask(
        url: String,
        workspace: File,
        credentialSetter: GitCredentialSetter,
        userName: String,
        projectName: String
    ) =
        FreshCheckoutTask(
            projectName = projectName,
            userName = userName,
            url = url,
            branchName = branchName,
            revision = revision,
            workspace = workspace,
            credentialSetter = credentialSetter,
            convertSubmoduleUrl = convertSubmoduleUrl,
            enableSubmodule = enableSubmodule,
            enableVirtualMergeBranch = enableVirtualMergeBranch,
            modeType = modeType,
            modeValue = modeValue,
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig,
            gitType = gitType,
            variables = taskParams
        )

    private fun updateTask(
        url: String,
        workspace: File,
        credentialSetter: GitCredentialSetter,
        userName: String,
        projectName: String
    ) =
        GitUpdateTask(
            projectName = projectName,
            userName = userName,
            url = url,
            branchName = branchName,
            revision = revision,
            workspace = workspace,
            credentialSetter = credentialSetter,
            convertSubmoduleUrl = convertSubmoduleUrl,
            enableSubmodule = enableSubmodule,
            enableVirtualMergeBranch = enableVirtualMergeBranch,
            modeType = modeType,
            modeValue = modeValue,
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig,
            gitType = gitType,
            variables = taskParams
        )

    private fun revertCheckoutTask(
        url: String,
        workspace: File,
        credentialSetter: GitCredentialSetter,
        userName: String,
        projectName: String
    ) =
        RevertCheckoutTask(
            projectName = projectName,
            userName = userName,
            url = url,
            branchName = branchName,
            revision = revision,
            workspace = workspace,
            credentialSetter = credentialSetter,
            convertSubmoduleUrl = convertSubmoduleUrl,
            enableSubmodule = enableSubmodule,
            enableVirtualMergeBranch = enableVirtualMergeBranch,
            modeType = modeType,
            modeValue = modeValue,
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig,
            gitType = gitType,
            variables = taskParams
        )
}