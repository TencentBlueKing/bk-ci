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

package com.tencent.devops.plugin.worker.task.scm.git

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ScmException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.plugin.worker.task.scm.util.SSHAgentUtils
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.code.git.CodeGitCredentialSetter
import com.tencent.devops.scm.code.git.CodeGitOauthCredentialSetter
import com.tencent.devops.scm.code.git.CodeGitUsernameCredentialSetter
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.scm.OauthSDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GIT_CREDENTIAL_ILLEGAL
import com.tencent.devops.worker.common.env.AgentEnv
import java.io.File
import org.slf4j.LoggerFactory

open class CodeGitPullCodeSetting(
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
    override val enableVirtualMergeBranch: Boolean,
    override val modeType: String?,
    override val modeValue: String?,
    override val gitType: ScmType,
    override val convertSubmoduleUrl: Boolean = true
) : DefaultGitPullCodeSetting(
    pipelineId = pipelineId,
    buildId = buildId,
    repositoryConfig = repositoryConfig,
    branchName = branchName,
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

    private val oauthResourceApi = ApiFactory.create(OauthSDKApi::class)

    override fun pullCode(): Map<String, String>? {
        val repo = getRepository() as CodeGitRepository
        return when (repo.authType) {
            RepoAuthType.OAUTH -> doOauthPullCode(repo)
            RepoAuthType.HTTP -> doHttpPullCode(repo)
            RepoAuthType.HTTPS -> doHttpPullCode(repo)
            else -> doPullCode(repo)
        }
    }

    private fun doPullCode(repo: CodeGitRepository): Map<String, String>? {

        val workspace = getCodeSourceDir(path)
        val credentialSetter = getSSHCredentialSetter(repo)

        return pullGitCode(
            repo = repo,
            workspace = workspace,
            credentialSetter = credentialSetter
        )
    }

    private fun doOauthPullCode(repo: CodeGitRepository): Map<String, String>? {
        val token = oauthResourceApi.get(repo.userName).data?.accessToken
            ?: throw ScmException("cannot found oauth access token for user(${repo.userName})", ScmType.CODE_GIT.name)

        val workspace = getCodeSourceDir(path)

        val credentialSetter = CodeGitOauthCredentialSetter(token)

        return pullGitCode(
            repo = repo,
            workspace = workspace,
            credentialSetter = credentialSetter
        )
    }

    private fun doHttpPullCode(repo: CodeGitRepository): Map<String, String>? {
        val credentials = getCredential(repo.credentialId)

        val message = MessageUtil.getMessageByLocale(GIT_CREDENTIAL_ILLEGAL, AgentEnv.getLocaleLanguage())
        if (credentials.size < CredentialSize) {
            logger.warn("The git credential($credentials) is illegal")
            throw ScmException(message, ScmType.CODE_GIT.name)
        }

        val username = credentials[1]
        val password = credentials[2]
        if (username.isEmpty() || password.isEmpty()) {
            logger.warn("The git credential username($username) or password($password) is empty")
            throw ScmException(message, ScmType.CODE_GIT.name)
        }

        val workspace = getCodeSourceDir(path)

        val credentialSetter = CodeGitUsernameCredentialSetter(username, password)

        return pullGitCode(
            repo = repo,
            workspace = workspace,
            credentialSetter = credentialSetter
        )
    }

    private fun getSSHCredentialSetter(repo: CodeGitRepository): CodeGitCredentialSetter {

        val credentials = getCredential(repo.credentialId)

        if (credentials.size <= 1) {
            logger.warn("The git credential($credentials) is illegal")
            throw ScmException("The git credential is illegal", ScmType.CODE_GIT.name)
        }
        val privateKey = credentials[1]

        val passPhrase = if (credentials.size >= CredentialSize) {
            val c = credentials[2]
            if (c.isEmpty()) {
                null
            } else {
                c
            }
        } else {
            null
        }

        SSHAgentUtils(privateKey, passPhrase).addIdentity()

        return CodeGitCredentialSetter(privateKey, passPhrase)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitPullCodeSetting::class.java)
        private const val CredentialSize = 3
    }
}
