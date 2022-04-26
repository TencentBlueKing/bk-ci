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

package com.tencent.devops.stream.trigger.git.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitChangeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitProjectInfo
import com.tencent.devops.stream.trigger.service.StreamTriggerTokenService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TXTGitApiService @Autowired constructor(
    private val client: Client,
    private val streamTriggerTokenService: StreamTriggerTokenService
) : TGitApiService(client) {

    override fun getGitProjectInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): TGitProjectInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get project $gitProjectId fail",
            apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR
        ) {
            client.getScm(ServiceGitCiResource::class).getProjectInfo(
                accessToken = cred.toToken(),
                gitProjectId = gitProjectId,
                useAccessToken = cred.toTokenType() == TokenTypeEnum.OAUTH
            ).data
        }?.let {
            TGitProjectInfo(
                gitProjectId = it.gitProjectId.toString(),
                defaultBranch = it.defaultBranch,
                gitHttpUrl = it.gitHttpUrl,
                name = it.name,
                gitSshUrl = it.gitSshUrl,
                homepage = it.homepage,
                gitHttpsUrl = it.gitHttpsUrl,
                description = it.description,
                avatarUrl = it.avatarUrl,
                pathWithNamespace = it.pathWithNamespace,
                nameWithNamespace = it.nameWithNamespace
            )
        }
    }

    /**
     * 获取两个commit之间的差异文件
     * @param from 旧commit
     * @param to 新commit
     * @param straight true：两个点比较差异，false：三个点比较差异。默认是 false
     */
    override fun getCommitChangeList(
        cred: TGitCred,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean,
        page: Int,
        pageSize: Int,
        retry: ApiRequestRetryInfo
    ): List<TGitChangeFileInfo> {
        return doRetryFun(
            retry = retry,
            log = "getCommitChangeFileListRetry from: $from to: $to error",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_CHANGE_FILE_LIST_ERROR
        ) {
            client.getScm(ServiceGitCiResource::class).getCommitChangeFileList(
                token = cred.toToken(),
                gitProjectId = gitProjectId,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            ).data ?: emptyList()
        }.map { TGitChangeFileInfo(it) }
    }

    override fun addMrComment(cred: TGitCred, gitProjectId: String, mrId: Long, mrBody: String) {
        return client.get(ServiceGitResource::class).addMrComment(
            token = streamTriggerTokenService.getGitProjectToken(gitProjectId) ?: cred.toToken(),
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = mrBody,
            tokenType = cred.toTokenType()
        )
    }
}
