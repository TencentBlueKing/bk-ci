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

package com.tencent.devops.scm.code.git

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.code.git.api.GitApi
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.RevisionInfo
import org.slf4j.LoggerFactory

class CodeGitlabScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val token: String,
    private val apiUrl: String
) : IScm {

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
            gitApi.addWebhook(apiUrl, token, projectName, hookUrl, null)
        } catch (e: ScmException) {
            throw ScmException("GitLab Token不正确", ScmType.CODE_GITLAB.name)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitlabScmImpl::class.java)
        private val gitApi = GitApi()
    }
}