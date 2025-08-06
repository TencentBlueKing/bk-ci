/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.plugin.worker.task.scm.git

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.scm.github.GitHubCredentialSetter
import java.io.File

class GithubPullCodeSetting(
    override val pipelineId: String,
    override val buildId: String,
    override val repositoryConfig: RepositoryConfig,
    override val strategy: CodePullStrategy?,
    override val workspace: File,
    override val path: String?,
    override val enableSubmodule: Boolean,
    override val taskParams: Map<String, String>,
    override val enableVirtualMergeBranch: Boolean,
    override val modeType: String?,
    override val modeValue: String?,
    override val gitType: ScmType,
    override val revision: String?,
    override val convertSubmoduleUrl: Boolean = false
) : DefaultGitPullCodeSetting(
    pipelineId = pipelineId,
    buildId = buildId,
    repositoryConfig = repositoryConfig,
    branchName = null,
    revision = revision,
    strategy = strategy,
    workspace = workspace,
    path = path,
    enableSubmodule = enableSubmodule,
    taskParams = taskParams,
    enableVirtualMergeBranch = enableVirtualMergeBranch,
    modeType = modeType,
    modeValue = modeValue,
    gitType = gitType,
    convertSubmoduleUrl = convertSubmoduleUrl
) {

    override fun pullCode(): Map<String, String>? {
        val repo = getRepository() as GithubRepository

        val workspace = getCodeSourceDir(path)

        val token: String? = try {
            getCredential(repo.credentialId)[0]
        } catch (ignored: Throwable) {
            null
        }

        return pullGitCode(
            repo = repo,
            workspace = workspace,
            credentialSetter = GitHubCredentialSetter(token)
        )
    }
}
