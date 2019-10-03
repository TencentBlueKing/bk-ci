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

package com.tencent.devops.repository.iscm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.config.GitConfig
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

class CodeGitlabScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val token: String,
    gitConfig: GitConfig
) : IScm {

    private val apiUrl = GitUtils.getGitApiUrl(apiUrl = gitConfig.gitlabApiUrl, repoUrl = url)

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "master"
        val gitBranch = gitApi.getBranch(apiUrl, token, projectName, branch)
        return RevisionInfo(
            gitBranch.commit.id,
            gitBranch.commit.message,
            branch
        )
    }

    override fun getBranches() =
        gitApi.listBranches(apiUrl, token, projectName)

    override fun getTags() =
        gitApi.listTags(apiUrl, token, projectName)

    override fun checkTokenAndPrivateKey() {
        try {
            getBranches()
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the gitlab token", ignored)
            throw ScmException("Gitlab access token 不正确", ScmType.CODE_GITLAB.name)
        }
    }

    override fun checkTokenAndUsername() {
        try {
            getBranches()
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the gitlab token", ignored)
            throw ScmException("Gitlab access token 不正确", ScmType.CODE_GITLAB.name)
        }
    }

    override fun addWebHook(hookUrl: String) {
        if (token.isEmpty()) {
            throw ScmException("GitLab Token为空", ScmType.CODE_GITLAB.name)
        }
        if (hookUrl.isEmpty()) {
            throw ScmException("GitLab hook url为空", ScmType.CODE_GITLAB.name)
        }
        try {
            logger.info("[HOOK_API]|$apiUrl")
            gitApi.addWebhook(apiUrl, token, projectName, hookUrl, null)
        } catch (e: ScmException) {
            throw ScmException("GitLab Token不正确", ScmType.CODE_GITLAB.name)
        }
    }

    override fun addCommitCheck(
        commitId: String,
        state: String,
        targetUrl: String,
        context: String,
        description: String,
        block: Boolean
    ) {
    }

    override fun addMRComment(mrId: Long, comment: String) {
    }

    override fun lock(repoName: String, applicant: String, subpath: String) {
        logger.info("gitlab can not lock")
    }

    override fun unlock(repoName: String, applicant: String, subpath: String) {
        logger.info("gitlab can not unlock")
    }

    override fun createBranch(branch: String, ref: String) {
        if (branch.isEmpty()) {
            throw RuntimeException("Git branch为空")
        }
        if (ref.isEmpty()) {
            throw RuntimeException("Git ref为空")
        }
        try {
            gitApi.createBranch(apiUrl, token, projectName, branch, ref)
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        }
    }

    override fun deleteBranch(branch: String) {
        if (branch.isEmpty()) {
            throw RuntimeException("Git branch为空")
        }
        try {
            gitApi.deleteBranch(apiUrl, token, projectName, branch)
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        }
    }

    override fun getCommits(branch: String?, all: Boolean, page: Int, size: Int): List<GitCommit> {
        return gitApi.listCommits(apiUrl, branch, token, projectName, all, page, size)
    }

    override fun getCommitDiff(sha: String): List<GitDiff> {
        return gitApi.getCommitDiff(apiUrl, sha, token, projectName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitlabScmImpl::class.java)
        private val gitApi = GitApi()
    }
}