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

package com.tencent.devops.scm.code

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.GITLAB_INVALID
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_ACCESS_CHECK_FAIL
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.code.git.CodeGitCredentialSetter
import com.tencent.devops.scm.code.git.api.GitApi
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import java.net.URLEncoder
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory

@SuppressWarnings("TooManyFunctions")
class CodeGitlabScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private var privateKey: String? = null,
    private var passPhrase: String? = null,
    private val token: String,
    gitConfig: GitConfig,
    private val event: String? = null
) : IScm {

    private val apiUrl = GitUtils.getGitApiUrl(apiUrl = gitConfig.gitlabApiUrl, repoUrl = url)

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "master"
        val gitBranch = gitApi.getBranch(host = apiUrl, token = token, projectName = projectName, branchName = branch)
        return RevisionInfo(
            revision = gitBranch.commit.id,
            updatedMessage = gitBranch.commit.message,
            branchName = branch,
            authorName = gitBranch.commit.authorName
        )
    }

    override fun getBranches(
        search: String?,
        page: Int,
        pageSize: Int
    ) =
        gitApi.listBranches(
            host = apiUrl,
            token = token,
            projectName = projectName,
            search = search,
            page = page,
            pageSize = pageSize
        )

    override fun getTags(search: String?) =
        gitApi.listTags(
            host = apiUrl,
            token = token,
            projectName = projectName,
            search = search
        )

    override fun checkTokenAndPrivateKey() {
        try {
            getBranches()
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the gitlab token", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    messageCode = USER_ACCESS_CHECK_FAIL
                ),
                ScmType.CODE_GITLAB.name
            )
        }
        if (privateKey != null) {
            try {
                // Check the private key
                val command = Git.lsRemoteRepository()
                val credentialSetter = CodeGitCredentialSetter(privateKey!!, passPhrase)
                credentialSetter.setGitCredential(command)
                command.setRemote(url).call()
            } catch (ignored: Throwable) {
                logger.warn("Fail to check the private key of git", ignored)
                throw ScmException(
                    ignored.message ?: I18nUtil.getCodeLanMessage(
                        GITLAB_INVALID
                    ),
                    ScmType.CODE_GITLAB.name
                )
            }
        }
    }

    override fun checkTokenAndUsername() {
        try {
            getBranches()
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the gitlab token", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    USER_ACCESS_CHECK_FAIL
                ),
                ScmType.CODE_GITLAB.name
            )
        }
    }

    override fun addWebHook(hookUrl: String) {
        if (token.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GITLAB_TOKEN_EMPTY
                ),
                ScmType.CODE_GITLAB.name
            )
        }
        if (hookUrl.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GITLAB_HOOK_URL_EMPTY
                ),
                ScmType.CODE_GITLAB.name
            )
        }
        try {
            logger.info("[HOOK_API]|$apiUrl")
            gitApi.addWebhook(apiUrl, token, projectName, hookUrl, event)
        } catch (ignored: Throwable) {
            logger.warn("Fail to add webhook of git", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GITLAB_TOKEN_FAIL
                ),
                ScmType.CODE_GITLAB.name
            )
        }
    }

    override fun addCommitCheck(
        commitId: String,
        state: String,
        targetUrl: String,
        context: String,
        description: String,
        block: Boolean,
        targetBranch: List<String>?
    ) = Unit

    override fun addMRComment(mrId: Long, comment: String) = Unit

    override fun lock(repoName: String, applicant: String, subpath: String) {
        logger.info("gitlab can not lock")
    }

    override fun unlock(repoName: String, applicant: String, subpath: String) {
        logger.info("gitlab can not unlock")
    }

    override fun getMergeRequestChangeInfo(mrId: Long): GitMrChangeInfo {
        val url = "projects/${URLEncoder.encode(projectName, "UTF-8")}/merge_requests/$mrId/changes"
        return gitApi.getMergeRequestChangeInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getMrInfo(mrId: Long): GitMrInfo {
        val url = "projects/${URLEncoder.encode(projectName, "UTF-8")}/merge_requests/$mrId"
        return gitApi.getMrInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getProjectInfo(projectName: String): GitProjectInfo {
        val url = "projects/${GitUtils.urlEncode(projectName)}"
        return gitApi.getProjectInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitlabScmImpl::class.java)
        private val gitApi = GitApi()
    }
}
