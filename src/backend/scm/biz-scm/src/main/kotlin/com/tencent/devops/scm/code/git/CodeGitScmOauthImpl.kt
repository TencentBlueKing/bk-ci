/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.scm.code.git

import com.tencent.devops.scm.code.git.api.CODE_GIT_URL
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.code.git.api.GitOauthApi
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.RevisionInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory

class CodeGitScmOauthImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val privateKey: String?,
    private val passPhrase: String?,
    private val token: String,
    private val event: String? = null
) : IScm {

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "master"
        val gitBranch = gitOauthApi.getBranch(CODE_GIT_URL, token, projectName, branch)
        return RevisionInfo(
                gitBranch.commit.id,
                gitBranch.commit.message,
                branch)
    }

    override fun getBranches() =
            gitOauthApi.listBranches(CODE_GIT_URL, token, projectName)

    override fun getTags() =
            gitOauthApi.listTags(CODE_GIT_URL, token, projectName)

    override fun checkTokenAndPrivateKey() {
        if (privateKey == null) {
            throw RuntimeException("私钥为空")
        }
        // Check if token legal
        try {
            getBranches()
        } catch (t: Throwable) {
            logger.warn("Fail to list all branches", t)
            throw RuntimeException("Git Token 不正确")
        }

        try {
            // Check the private key
            val command = Git.lsRemoteRepository()
            val credentialSetter = CodeGitCredentialSetter(privateKey, passPhrase)
            credentialSetter.setGitCredential(command)
            command.setRemote(url)
                    .call()
        } catch (e: Throwable) {
            logger.warn("Fail to check the private key of git", e)
            throw RuntimeException("Git 私钥不对")
        }
    }

    override fun checkTokenAndUsername() {
        if (privateKey == null) {
            throw RuntimeException("用户密码为空")
        }

        // Check if token legal
        try {
            getBranches()
        } catch (t: Throwable) {
            logger.warn("Fail to list all branches", t)
            throw RuntimeException("Git Token 不正确")
        }

        try {
            val command = Git.lsRemoteRepository()
            command.setRemote(url)
            command.setCredentialsProvider(UsernamePasswordCredentialsProvider(privateKey, passPhrase))
            command.call()
        } catch (t: Throwable) {
            logger.warn("Fail to check the username and password of git", t)
            throw RuntimeException("Git 用户名或者密码不对")
        }
    }

    override fun addWebHook(hookUrl: String) {
        if (token.isEmpty()) {
            throw RuntimeException("Git Token为空")
        }
        if (hookUrl.isEmpty()) {
            throw RuntimeException("Git hook url为空")
        }
        try {
            gitOauthApi.addWebhook(CODE_GIT_URL, token, projectName, hookUrl, event)
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        }
    }

    override fun addCommitCheck(commitId: String, state: String, targetUrl: String, context: String, description: String, block: Boolean) {
        if (token.isEmpty()) {
            throw RuntimeException("Git Token为空")
        }
        try {
            gitOauthApi.addCommitCheck(CODE_GIT_URL, token, projectName, commitId, state, targetUrl, context, description, block)
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        }
    }

    override fun addMRComment(mrId: Long, comment: String) {
        gitOauthApi.addMRComment(CODE_GIT_URL, token, projectName, mrId, comment)
    }

    override fun lock(repname: String, applicant: String, subpath: String) {
        logger.info("Git oauth can not lock")
    }

    override fun unlock(repname: String, applicant: String, subpath: String) {
        logger.info("Git oauth can not unlock")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitScmOauthImpl::class.java)
        private val gitOauthApi = GitOauthApi()
    }
}
