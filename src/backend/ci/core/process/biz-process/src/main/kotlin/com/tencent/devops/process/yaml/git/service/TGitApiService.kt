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

package com.tencent.devops.process.yaml.git.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitCommitInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitCred
import com.tencent.devops.process.yaml.git.pojo.PacGitPushResult
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitChangeFileInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitCommitInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitCred
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitFileInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitMrChangeInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitMrInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitProjectInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitPushResult
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitTreeFileInfo
import com.tencent.devops.process.yaml.git.service.PacApiUtil.doRetryFun
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.scm.pojo.GitCreateBranch
import com.tencent.devops.scm.pojo.GitCreateMergeRequest
import com.tencent.devops.scm.pojo.GitListMergeRequest
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.MergeRequestState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.core.Response

@Service
class TGitApiService @Autowired constructor(
    private val client: Client
) : PacGitApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitApiService::class.java)
    }

    /**
     * 通过凭据获取可以直接使用的token
     */
    override fun getToken(
        cred: PacGitCred
    ): String {
        return cred.toToken()
    }

    override fun getGitProjectInfo(
        cred: PacGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): TGitProjectInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get project $gitProjectId fail",
            errorCode = ProcessMessageCode.ERROR_GET_GIT_PROJECT_INFO
        ) {
            client.get(ServiceGitResource::class).getProjectInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                gitProjectId = gitProjectId
            ).data
        }?.let {
            TGitProjectInfo(it)
        }
    }

    override fun getGitCommitInfo(
        cred: PacGitCred,
        gitProjectId: String,
        sha: String,
        retry: ApiRequestRetryInfo
    ): PacGitCommitInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get commit info $sha fail",
            errorCode = ProcessMessageCode.ERROR_GET_COMMIT_INFO
        ) {
            client.get(ServiceGitResource::class).getRepoRecentCommitInfo(
                repoName = gitProjectId,
                sha = sha,
                token = cred.toToken(),
                tokenType = cred.toTokenType()
            ).data
        }?.let { TGitCommitInfo(it) }
    }

    override fun getMrInfo(
        cred: PacGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): TGitMrInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId info error",
            errorCode = ProcessMessageCode.ERROR_GET_GIT_MERGE_INFO
        ) {
            client.get(ServiceGitResource::class).getMergeRequestInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                repoName = gitProjectId,
                mrId = mrId.toLong()
            ).data
        }?.let {
            TGitMrInfo(
                mergeStatus = it.mergeStatus ?: "",
                baseCommit = it.baseCommit,
                baseInfo = it
            )
        }
    }

    fun getMrReview(
        cred: PacGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): GitMrReviewInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId info error",
            errorCode = ProcessMessageCode.ERROR_GET_GIT_MERGE_REVIEW
        ) {
            client.get(ServiceGitResource::class).getMergeRequestReviewersInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                repoName = gitProjectId,
                mrId = mrId.toLong()
            ).data
        }
    }

    override fun getMrChangeInfo(
        cred: PacGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): TGitMrChangeInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId changeInfo error",
            errorCode = ProcessMessageCode.ERROR_GET_GIT_MERGE_CHANGE
        ) {
            client.get(ServiceGitResource::class).getMergeRequestChangeInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                repoName = gitProjectId,
                mrId = mrId.toLong()
            ).data
        }?.let {
            TGitMrChangeInfo(
                files = it.files?.map { f ->
                    TGitChangeFileInfo(f)
                } ?: emptyList()
            )
        }
    }

    override fun getFileTree(
        cred: PacGitCred,
        gitProjectId: String,
        path: String?,
        ref: String?,
        recursive: Boolean,
        retry: ApiRequestRetryInfo
    ): List<TGitTreeFileInfo> {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get $path file tree error",
            errorCode = ProcessMessageCode.ERROR_GET_GIT_FILE_TREE
        ) {
            client.get(ServiceGitResource::class).getGitFileTree(
                gitProjectId = gitProjectId,
                path = path ?: "",
                token = cred.toToken(),
                ref = ref,
                recursive = recursive,
                tokenType = cred.toTokenType()
            ).data ?: emptyList()
        }.map { TGitTreeFileInfo(it) }
    }

    override fun getFileContent(
        cred: PacGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String,
        retry: ApiRequestRetryInfo
    ): String {
        cred as TGitCred
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get yaml $fileName from $ref fail",
            errorCode = ProcessMessageCode.ERROR_GET_YAML_CONTENT
        ) {
            client.get(ServiceGitResource::class).getGitFileContent(
                token = cred.toToken(),
                authType = if (cred.useAccessToken) {
                    RepoAuthType.OAUTH
                } else {
                    RepoAuthType.SSH
                },
                repoName = gitProjectId,
                ref = getTriggerBranch(ref),
                filePath = fileName
            ).data!!
        }
    }

    private fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    override fun getFileInfo(
        cred: PacGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): TGitFileInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "getFileInfo: [$gitProjectId|$fileName][$ref] error",
            errorCode = ProcessMessageCode.ERROR_GET_GIT_FILE_INFO
        ) {
            client.get(ServiceGitResource::class).getGitFileInfo(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = cred.toToken(),
                ref = ref,
                tokenType = cred.toTokenType()
            ).data
        }?.let { TGitFileInfo(content = it.content ?: "", blobId = it.blobId) }
    }

    @Suppress("ComplexMethod")
    override fun pushYamlFile(
        cred: PacGitCred,
        gitProjectId: String,
        defaultBranch: String,
        filePath: String,
        content: String,
        commitMessage: String,
        targetAction: CodeTargetAction,
        pipelineId: String,
        pipelineName: String,
        versionName: String?
    ): PacGitPushResult {
        val token = cred.toToken()
        val branchName = if (targetAction == CodeTargetAction.COMMIT_TO_MASTER) {
            defaultBranch
        } else {
            versionName!!
        }
        // 1. 判断分支是否存在
        val branchExists = client.get(ServiceGitResource::class).getBranch(
            accessToken = token,
            userId = "",
            repository = gitProjectId,
            page = PageUtil.DEFAULT_PAGE,
            pageSize = PageUtil.DEFAULT_PAGE_SIZE,
            search = branchName
        ).data?.any { it.name == branchName } ?: false
        // 分支不存在,则需要创建
        if (!branchExists) {
            client.get(ServiceGitResource::class).createBranch(
                token = token,
                tokenType = cred.toTokenType(),
                gitProjectId = gitProjectId,
                gitCreateBranch = GitCreateBranch(
                    branchName = branchName,
                    ref = defaultBranch
                )
            ).data
        }
        // 2. 判断文件是否存在
        val fileExists = try {
            client.get(ServiceGitResource::class).getGitFileInfo(
                gitProjectId = gitProjectId,
                filePath = filePath,
                token = token,
                ref = branchName,
                tokenType = cred.toTokenType()
            ).data
            true
        } catch (ignored: Exception) {
            false
        }
        val gitOperationFile = GitOperationFile(
            filePath = filePath,
            branch = branchName,
            content = content,
            commitMessage = commitMessage
        )
        if (fileExists) {
            client.get(ServiceGitResource::class).gitUpdateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitOperationFile = gitOperationFile,
                tokenType = cred.toTokenType()
            )
        } else {
            client.get(ServiceGitResource::class).gitCreateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitOperationFile = gitOperationFile,
                tokenType = cred.toTokenType()
            )
        }
        val mrUrl = if (targetAction == CodeTargetAction.PUSH_BRANCH_AND_REQUEST_MERGE ||
            targetAction == CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE
        ) {
            createYamlMergeRequest(
                fileExists = fileExists,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                token = token,
                cred = cred,
                gitProjectId = gitProjectId,
                branchName = branchName,
                defaultBranch = defaultBranch
            )
        } else {
            null
        }
        val fileInfo = getFileInfo(
            cred = cred,
            gitProjectId = gitProjectId,
            fileName = filePath,
            ref = branchName,
            retry = ApiRequestRetryInfo(true)
        )
        val commitInfo = getGitCommitInfo(
            cred = cred,
            gitProjectId = gitProjectId,
            sha = branchName,
            retry = ApiRequestRetryInfo(true)
        )
        return TGitPushResult(
            filePath = filePath,
            branch = branchName,
            blobId = fileInfo!!.blobId,
            lastCommitId = commitInfo!!.commitId,
            mrUrl = mrUrl
        )
    }

    private fun createYamlMergeRequest(
        fileExists: Boolean,
        pipelineId: String,
        pipelineName: String,
        token: String,
        cred: PacGitCred,
        gitProjectId: String,
        branchName: String,
        defaultBranch: String
    ): String? {
        val dateStr = DateTimeUtil.toDateTime(LocalDateTime.now())
        val title = if (fileExists) {
            I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_MERGE_YAML_UPDATE_FILE_TITLE,
                params = arrayOf(dateStr, pipelineName),
                language = I18nUtil.getDefaultLocaleLanguage()
            )
        } else {
            I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_MERGE_YAML_CREATE_FILE_TITLE,
                params = arrayOf(dateStr, pipelineId),
                language = I18nUtil.getDefaultLocaleLanguage()
            )
        }
        val mrList = client.get(ServiceGitResource::class).listMergeRequest(
            token = token,
            tokenType = cred.toTokenType(),
            gitProjectId = gitProjectId,
            gitListMergeRequest = GitListMergeRequest(
                sourceBranch = branchName,
                targetBranch = defaultBranch,
                state = MergeRequestState.OPENED.value
            )
        ).data ?: emptyList()
        // 如果源分支->目标分支的mr已存在,则不需要再发起mr
        val mrNumber = if (mrList.isNotEmpty()) {
            mrList[0].mrNumber
        } else {
            val mrInfo = client.get(ServiceGitResource::class).createMergeRequest(
                token = token,
                tokenType = cred.toTokenType(),
                gitProjectId = gitProjectId,
                gitCreateMergeRequest = GitCreateMergeRequest(
                    sourceBranch = branchName,
                    targetBranch = defaultBranch,
                    title = title
                )
            ).data
            mrInfo?.mrNumber
        } ?: return null
        val projectInfo = getGitProjectInfo(
            cred = cred,
            gitProjectId = gitProjectId,
            retry = ApiRequestRetryInfo(true)
        ) ?: return null
        return "${projectInfo.homepage}/merge_requests/$mrNumber"
    }

    private fun PacGitCred.toToken(): String {
        this as TGitCred
        if (this.accessToken != null) {
            return this.accessToken
        }
        return client.get(ServiceOauthResource::class).gitGet(this.userId!!).data?.accessToken
            ?: throw CustomException(
                Response.Status.FORBIDDEN,
                "STEAM PROJECT ENABLE USER NO OAUTH PERMISSION"
            )
    }

    private fun PacGitCred.toTokenType(): TokenTypeEnum {
        this as TGitCred
        return if (this.useAccessToken) {
            TokenTypeEnum.OAUTH
        } else {
            TokenTypeEnum.PRIVATE_KEY
        }
    }
}
