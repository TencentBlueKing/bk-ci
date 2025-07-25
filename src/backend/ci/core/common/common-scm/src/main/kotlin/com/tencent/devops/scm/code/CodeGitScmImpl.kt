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

package com.tencent.devops.scm.code

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.code.git.CodeGitCredentialSetter
import com.tencent.devops.scm.code.git.api.GitApi
import com.tencent.devops.scm.code.git.api.GitHook
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.exception.GitApiException
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitCommitReviewInfo
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.GitTagInfo
import com.tencent.devops.scm.pojo.LoginSession
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TapdWorkItem
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.scm.utils.code.git.GitUtils.urlEncode
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory

@Suppress("ALL")
class CodeGitScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val privateKey: String?,
    private val passPhrase: String?,
    private val token: String,
    private val gitConfig: GitConfig,
    private val gitApi: GitApi,
    private val event: String? = null
) : IScm {

    private val apiUrl = gitConfig.gitApiUrl

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "master"
        val gitBranch = gitApi.getBranch(
            host = apiUrl,
            token = token,
            projectName = projectName,
            branchName = branch
        )
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
        if (privateKey == null) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.SERCRT_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        // Check if token legal
        try {
            getBranches()
        } catch (ignored: Throwable) {
            logger.warn("Fail to list all branches", ignored)
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.THIRD_PARTY_SERVICE_OPERATION_FAILED,
                    params = arrayOf(ScmType.CODE_GIT.name, ignored.message ?: "")
                ),
                ScmType.CODE_GIT.name
            )
        }

        try {
            // Check the private key
            val command = Git.lsRemoteRepository()
            val credentialSetter = CodeGitCredentialSetter(privateKey, passPhrase)
            credentialSetter.setGitCredential(command)
            command.setRemote(url).call()
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the private key of git", ignored)
            throw ScmException(
                GitUtils.matchExceptionCode(ignored.message ?: "")?.let {
                    I18nUtil.getCodeLanMessage(it)
                } ?: ignored.message ?: I18nUtil.getCodeLanMessage(CommonMessageCode.GIT_SERCRT_WRONG),
                ScmType.CODE_GIT.name
            )
        }
    }

    override fun checkTokenAndUsername() {
        if (privateKey == null) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.PWD_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }

        // Check if token legal
        try {
            getBranches()
        } catch (ignored: Throwable) {
            logger.warn("Fail to list all branches", ignored)
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.THIRD_PARTY_SERVICE_OPERATION_FAILED,
                    params = arrayOf(ScmType.CODE_GIT.name, ignored.message ?: "")
                ),
                ScmType.CODE_GIT.name
            )
        }

        try {
            val command = Git.lsRemoteRepository()
            command.setRemote(url)
            command.setCredentialsProvider(UsernamePasswordCredentialsProvider(privateKey, passPhrase))
            command.call()
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the username and password of git", ignored)
            throw ScmException(
                GitUtils.matchExceptionCode(ignored.message ?: "")?.let {
                    I18nUtil.getCodeLanMessage(it)
                } ?: ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_LOGIN_FAIL
                ),
                ScmType.CODE_GIT.name
            )
        }
    }

    override fun addWebHook(hookUrl: String) {
        if (token.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        if (hookUrl.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_HOOK_URL_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        try {
            gitApi.addWebhook(apiUrl, token, projectName, hookUrl, event)
        } catch (ignored: Throwable) {
            logger.warn("Fail to add webhook of git", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_FAIL
                ),
                ScmType.CODE_GIT.name
            )
        }
    }

    override fun getWebHooks(): List<GitHook> {
        if (token.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        try {
            return gitApi.getHooks(apiUrl, token, projectName)
        } catch (ignored: Throwable) {
            logger.warn("Fail to get webhook of git", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_FAIL
                ),
                ScmType.CODE_GIT.name
            )
        }
    }

    override fun updateWebHook(hookId: Long, hookUrl: String) {
        if (token.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        if (hookUrl.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_HOOK_URL_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        try {
            gitApi.updateHook(
                host = apiUrl,
                hookId = hookId,
                token = token,
                projectName = projectName,
                hookUrl = hookUrl,
                event = event
            )
        } catch (ignored: Throwable) {
            logger.warn("Fail to update webhook of git", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_FAIL
                ),
                ScmType.CODE_GIT.name
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
    ) {
        if (token.isEmpty()) {
            throw ScmException(
                I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_EMPTY
                ),
                ScmType.CODE_GIT.name
            )
        }
        try {
            gitApi.addCommitCheck(
                host = apiUrl,
                token = token,
                projectName = projectName,
                commitId = commitId,
                state = state,
                detailUrl = targetUrl,
                context = context,
                description = description,
                block = block,
                targetBranch = targetBranch
            )
        } catch (e: GitApiException) {
            throw e
        } catch (ignored: Throwable) {
            logger.warn("Fail to add commit check of git", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.GIT_TOKEN_FAIL
                ),
                ScmType.CODE_GIT.name
            )
        }
    }

    override fun addMRComment(mrId: Long, comment: String) {
        gitApi.addMRComment(apiUrl, token, projectName, mrId, comment)
    }

    override fun lock(repoName: String, applicant: String, subpath: String) {
        logger.info("Git can not lock")
    }

    override fun unlock(repoName: String, applicant: String, subpath: String) {
        logger.info("Git can not unlock")
    }

    override fun getMergeRequestChangeInfo(mrId: Long): GitMrChangeInfo {
        val url = "projects/${urlEncode(projectName)}/merge_request/$mrId/changes"
        return gitApi.getMergeRequestChangeInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getMrInfo(mrId: Long): GitMrInfo {
        val url = "projects/${urlEncode(projectName)}/merge_request/$mrId"
        return gitApi.getMrInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getMrReviewInfo(mrId: Long): GitMrReviewInfo {
        val url = "projects/${urlEncode(projectName)}/merge_request/$mrId/review"
        return gitApi.getMrReviewInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getMrCommitList(mrId: Long, page: Int, size: Int): List<GitCommit> {
        val url = "projects/${urlEncode(projectName)}/merge_request/$mrId/commits"
        return gitApi.getMrCommitList(
            host = apiUrl,
            token = token,
            url = url,
            page = page,
            size = size
        )
    }

    override fun getProjectInfo(projectName: String): GitProjectInfo {
        val url = "projects/${urlEncode(projectName)}"
        return gitApi.getProjectInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getCommitReviewInfo(crId: Long): GitCommitReviewInfo {
        val url = "projects/${urlEncode(projectName)}/review/$crId"
        return gitApi.getCommitReviewInfo(
            host = apiUrl,
            token = token,
            url = url
        )
    }

    override fun getLoginSession(): LoginSession? {
        val url = "session"
        return gitApi.getGitSession(
            host = apiUrl,
            url = url,
            username = privateKey!!,
            password = passPhrase!!
        )
    }

    override fun getTag(tagName: String): GitTagInfo? {
        val url = "projects/${urlEncode(projectName)}/repository/tags/${urlEncode(tagName)}"
        return gitApi.getTagInfo(
            host = apiUrl,
            url = url,
            token = token
        )
    }

    override fun getTapdWorkItems(refType: String, iid: Long): List<TapdWorkItem> {
        return gitApi.getTapdWorkitems(
            host = apiUrl,
            token = token,
            id = projectName,
            type = refType,
            iid = iid
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitScmImpl::class.java)
    }
}
