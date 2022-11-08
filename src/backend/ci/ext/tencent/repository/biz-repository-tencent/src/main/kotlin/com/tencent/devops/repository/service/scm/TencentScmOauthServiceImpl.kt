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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.api.ServiceScmOauthResource
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentScmOauthServiceImpl @Autowired constructor(val client: Client) : IScmOauthService {

    override fun getLatestRevision(
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): RevisionInfo {
        return client.getScm(ServiceScmOauthResource::class).getLatestRevision(
            projectName = projectName,
            url = url,
            type = type,
            branchName = branchName,
            privateKey = privateKey,
            passPhrase = passPhrase,
            token = token,
            region = region,
            userName = userName
        ).data!!
    }

    override fun listBranches(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?,
        search: String?
    ): List<String> {
        return client.getScm(ServiceScmOauthResource::class).listBranches(
            projectName = projectName,
            url = url,
            type = type,
            privateKey = privateKey,
            passPhrase = passPhrase,
            token = token,
            region = region,
            userName = userName,
            search = search
        ).data!!
    }

    override fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String,
        search: String?
    ): List<String> {
        return client.getScm(ServiceScmOauthResource::class).listTags(
            projectName = projectName,
            url = url,
            type = type,
            token = token,
            userName = userName,
            search = search
        ).data!!
    }

    override fun checkPrivateKeyAndToken(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): TokenCheckResult {
        return client.getScm(ServiceScmOauthResource::class).checkPrivateKeyAndToken(
            projectName = projectName,
            url = url,
            type = type,
            privateKey = privateKey,
            passPhrase = passPhrase,
            token = token,
            region = region,
            userName = userName
        ).data!!
    }

    override fun addWebHook(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        event: String?
    ) {
        client.getScm(ServiceScmOauthResource::class).addWebHook(
            projectName = projectName,
            url = url,
            type = type,
            privateKey = privateKey,
            passPhrase = passPhrase,
            token = token,
            region = region,
            userName = userName,
            event = event
        )
    }

    override fun getMergeRequestChangeInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrChangeInfo? {
        return client.getScm(ServiceScmOauthResource::class).getMergeRequestChangeInfo(
            projectName = projectName,
            url = url,
            type = type,
            token = token,
            mrId = mrId
        ).data
    }

    override fun getMrInfo(projectName: String, url: String, type: ScmType, token: String?, mrId: Long): GitMrInfo? {
        return client.getScm(ServiceScmOauthResource::class).getMrInfo(
            projectName = projectName,
            url = url,
            type = type,
            token = token,
            mrId = mrId
        ).data
    }

    override fun getMrReviewInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrReviewInfo? {
        return client.getScm(ServiceScmOauthResource::class).getMrReviewInfo(
            projectName = projectName,
            url = url,
            type = type,
            token = token,
            mrId = mrId
        ).data
    }

    override fun getMrCommitList(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long,
        page: Int,
        size: Int
    ): List<GitCommit> {
        return client.getScm(ServiceScmOauthResource::class).getMrCommitList(
            projectName = projectName,
            url = url,
            type = type,
            token = token,
            mrId = mrId,
            page = page,
            size = size
        ).data ?: emptyList()
    }

    override fun addCommitCheck(request: CommitCheckRequest) {
        client.getScm(ServiceScmOauthResource::class).addCommitCheck(request)
    }
}
