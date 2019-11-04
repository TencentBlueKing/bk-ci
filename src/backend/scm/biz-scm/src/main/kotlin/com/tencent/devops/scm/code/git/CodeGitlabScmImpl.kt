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

import com.tencent.devops.scm.code.git.api.GITLAB_URL
import com.tencent.devops.scm.code.git.api.GitApi
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.RevisionInfo
import org.slf4j.LoggerFactory

class CodeGitlabScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val token: String
) : IScm {

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "master"
        val gitBranch = gitApi.getBranch(GITLAB_URL, token, projectName, branch)
        return RevisionInfo(
                gitBranch.commit.id,
                gitBranch.commit.message,
                branch)
    }

    override fun getBranches() =
            gitApi.listBranches(GITLAB_URL, token, projectName)

    override fun getTags() =
            gitApi.listTags(GITLAB_URL, token, projectName)

    override fun checkTokenAndPrivateKey() {
        try {
            getBranches()
        } catch (t: Throwable) {
            logger.warn("Fail to check the gitlab token", t)
            throw RuntimeException("Gitlab access token 不正确")
        }
    }

    override fun checkTokenAndUsername() {
        try {
            getBranches()
        } catch (t: Throwable) {
            logger.warn("Fail to check the gitlab token", t)
            throw RuntimeException("Gitlab access token 不正确")
        }
    }

    override fun addWebHook(hookUrl: String) {
        if (token.isEmpty()) {
            throw RuntimeException("GitLab Token为空")
        }
        if (hookUrl.isEmpty()) {
            throw RuntimeException("GitLab hook url为空")
        }
        try {
            gitApi.addWebhook(GITLAB_URL, token, projectName, hookUrl, null)
        } catch (e: ScmException) {
            throw RuntimeException("GitLab Token不正确")
        }
    }

    override fun addCommitCheck(commitId: String, state: String, targetUrl: String, context: String, description: String, block: Boolean) {
    }

    override fun addMRComment(mrId: Long, comment: String) {
    }

    override fun lock(repname: String, applicant: String, subpath: String) {
        logger.info("gitlab can not lock")
    }

    override fun unlock(repname: String, applicant: String, subpath: String) {
        logger.info("gitlab can not unlock")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitlabScmImpl::class.java)
        private val gitApi = GitApi()
    }
}