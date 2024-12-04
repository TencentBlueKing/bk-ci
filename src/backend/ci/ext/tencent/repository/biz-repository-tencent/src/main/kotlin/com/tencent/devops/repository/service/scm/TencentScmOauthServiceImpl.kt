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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.service.TencentScmMonitorService
import com.tencent.devops.scm.api.ServiceScmOauthResource
import com.tencent.devops.scm.code.git.api.GitHook
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitCommitReviewInfo
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentScmOauthServiceImpl @Autowired constructor(
    val client: Client,
    val scmMonitorService: TencentScmMonitorService
) : IScmOauthService {

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
        search: String?,
        page: Int,
        pageSize: Int
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
            search = search,
            page = page,
            pageSize = pageSize
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

    override fun getWebHooks(projectName: String, url: String, type: ScmType, token: String?): List<GitHook> {
        return client.getScm(ServiceScmOauthResource::class).getWebHooks(
            projectName = projectName,
            url = url,
            type = type,
            token = token
        ).data ?: emptyList()
    }

    override fun updateWebHook(
        hookId: Long,
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        event: String?,
        hookUrl: String?
    ) {
        client.getScm(ServiceScmOauthResource::class).updateWebHook(
            hookId = hookId,
            projectName = projectName,
            url = url,
            type = type,
            privateKey = privateKey,
            passPhrase = passPhrase,
            token = token,
            region = region,
            userName = userName,
            event = event,
            hookUrl = hookUrl
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

    override fun getProjectInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?
    ): GitProjectInfo? {
        return client.getScm(ServiceScmOauthResource::class).getProjectInfo(
            projectName = projectName,
            url = url,
            type = type,
            token = token
        ).data
    }

    override fun getCommitReviewInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        crId: Long
    ): GitCommitReviewInfo? {
        return client.getScm(ServiceScmOauthResource::class).getCrReviewInfo(
            projectName = projectName,
            url = url,
            type = type,
            token = token,
            crId = crId
        ).data
    }

    override fun addCommitCheck(request: CommitCheckRequest) {
        val startEpoch = System.currentTimeMillis()
        var requestTime = System.currentTimeMillis()
        var responseTime = System.currentTimeMillis()
        var statusCode: Int = HTTP_200
        var statusMessage: String? = "OK"
        try {
            requestTime = System.currentTimeMillis() // 请求时间
            client.getScm(ServiceScmOauthResource::class).addCommitCheck(request)
            responseTime = System.currentTimeMillis() // 响应时间
        } catch (ignored: RemoteServiceException) {
            responseTime = System.currentTimeMillis() // 异常响应时间
            statusMessage = ignored.message
            statusCode = ignored.errorCode ?: 400
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(messageCode = CommonMessageCode.GIT_TOKEN_FAIL),
                ScmType.CODE_GIT.name
            )
        } finally {
            scmMonitorService.reportCommitCheck(
                requestTime = requestTime,
                responseTime = responseTime,
                statusCode = statusCode,
                statusMessage = statusMessage,
                projectName = request.projectName,
                commitId = request.commitId,
                block = request.block,
                targetUrl = request.targetUrl
            )
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add commit check")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TencentScmOauthServiceImpl::class.java)
    }
}
