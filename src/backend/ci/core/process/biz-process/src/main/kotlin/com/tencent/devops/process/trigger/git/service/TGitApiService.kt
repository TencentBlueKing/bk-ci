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

package com.tencent.devops.process.trigger.git.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.trigger.common.exception.ErrorCodeEnum
import com.tencent.devops.process.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.trigger.git.pojo.PacGitCred
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.process.trigger.git.service.PacApiUtil.doRetryFun
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitChangeFileInfo
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitFileInfo
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitMrChangeInfo
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitMrInfo
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitProjectInfo
import com.tencent.devops.process.trigger.git.pojo.tgit.TGitTreeFileInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
            apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR
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
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO
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
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO
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
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_CHANGE_INFO
        ) {
            client.get(ServiceGitResource::class).getMergeRequestChangeInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                repoName = gitProjectId,
                mrId = mrId.toLong()
            ).data
        }?.let {
            TGitMrChangeInfo(
                files = it.files.map { f ->
                    TGitChangeFileInfo(f)
                }
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
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR
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
            apiErrorCode = ErrorCodeEnum.GET_YAML_CONTENT_ERROR
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
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR
        ) {
            client.get(ServiceGitResource::class).getGitFileInfo(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = cred.toToken(),
                ref = ref,
                tokenType = cred.toTokenType()
            ).data
        }?.let { TGitFileInfo(content = it.content, blobId = it.blobId) }
    }


    protected fun PacGitCred.toToken(): String {
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

    protected fun PacGitCred.toTokenType(): TokenTypeEnum {
        this as TGitCred
        return if (this.useAccessToken) {
            TokenTypeEnum.OAUTH
        } else {
            TokenTypeEnum.PRIVATE_KEY
        }
    }
}
