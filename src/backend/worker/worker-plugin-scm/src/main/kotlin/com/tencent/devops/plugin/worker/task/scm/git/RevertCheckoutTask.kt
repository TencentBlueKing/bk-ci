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
import com.tencent.devops.scm.code.git.api.GitCredentialSetter
import com.tencent.devops.worker.common.logger.LoggerService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import java.io.File

class RevertCheckoutTask(
    override val projectName: String,
    override val userName: String,
    override val url: String,
    override val branchName: String?,
    override val revision: String?,
    override val workspace: File,
    override val credentialSetter: GitCredentialSetter,
    override val convertSubmoduleUrl: Boolean = true,
    override val enableSubmodule: Boolean,
    override val enableVirtualMergeBranch: Boolean,
    override val modeType: String?,
    override val modeValue: String?,
    override val pipelineId: String,
    override val buildId: String,
    override val repositoryConfig: RepositoryConfig,
    override val gitType: ScmType,
    override val variables: Map<String, String>
) : GitUpdateTask(
    projectName,
    userName,
    url,
    branchName,
    revision,
    workspace,
    credentialSetter,
    convertSubmoduleUrl,
    enableSubmodule,
    enableVirtualMergeBranch,
    modeType,
    modeValue,
    pipelineId,
    buildId,
    repositoryConfig,
    gitType,
    variables
) {

    override fun preUpdate(git: Git) {
        if (workspace.exists() && File(workspace.path, ".git").exists()) {
            val ref = git.reset().setMode(ResetCommand.ResetType.HARD).call()
            LoggerService.addNormalLine("Revert the git code to revision $ref")
            val clean = git.clean().setForce(true).setIgnore(false).call()
            clean?.forEach {
                LoggerService.addNormalLine("Remove the un-revision file $it")
            }
        }
    }
}