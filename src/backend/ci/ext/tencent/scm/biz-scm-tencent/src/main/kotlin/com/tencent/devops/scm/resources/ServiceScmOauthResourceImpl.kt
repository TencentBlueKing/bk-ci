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

package com.tencent.devops.scm.resources

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.api.ServiceScmOauthResource
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.RepositoryProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.services.ScmOauthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceScmOauthResourceImpl @Autowired constructor(private val scmOauthService: ScmOauthService) :
    ServiceScmOauthResource {

    override fun getLatestRevision(
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String?,
        additionalPath: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): Result<RevisionInfo> {
        logger.info("Start to get the code latest version of " +
            "(projectName=$projectName, url=$url, type=$type, branch=$branchName,username=$userName)"
        )
        return Result(
            scmOauthService.getLatestRevision(
                projectName = projectName,
                url = url,
                type = type,
                branchName = branchName,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName
            )
        )
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
    ): Result<List<String>> {
        logger.info(
            "Start to list the branches of " +
                "(projectName=$projectName, url=$url, type=$type, region=$region, username=$userName)"
        )
        return Result(
            scmOauthService.listBranches(
                projectName = projectName,
                url = url,
                type = type,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName,
                search = search
            )
        )
    }

    override fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String,
        search: String?
    ): Result<List<String>> {
        logger.info(
            "Start to list the branches of (projectName=$projectName, url=$url, type=$type, username=$userName)"
        )
        return Result(
            scmOauthService.listTags(
                projectName = projectName,
                url = url,
                type = type,
                token = token,
                userName = userName,
                search = search
            )
        )
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
    ): Result<TokenCheckResult> {
        logger.info(
            "Start to check the private key and token of " +
                "(projectName=$projectName, url=$url, type=$type, region=$region, username=$userName)"
        )
        return Result(
            scmOauthService.checkPrivateKeyAndToken(
                projectName = projectName,
                url = url,
                type = type,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName
            )
        )
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
    ): Result<Boolean> {
        logger.info(
            "Start to add the web hook of " +
                "(projectName=$projectName, url=$url, type=$type, username=$userName, event=$event)"
        )
        scmOauthService.addWebHook(
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
        return Result(true)
    }

    override fun addCommitCheck(
        request: CommitCheckRequest
    ): Result<Boolean> {
        logger.info("Start to add the commit check of request(${JsonUtil.skipLogFields(request)})")
        scmOauthService.addCommitCheck(request)
        return Result(true)
    }

    override fun getMergeRequestChangeInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): Result<GitMrChangeInfo?> {
        return Result(
            scmOauthService.getMergeRequestChangeInfo(
                projectName = projectName,
                url = url,
                type = type,
                token = token,
                mrId = mrId
            )
        )
    }

    override fun getMrInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): Result<GitMrInfo?> {
        return Result(
            scmOauthService.getMrInfo(
                projectName = projectName,
                url = url,
                type = type,
                token = token,
                mrId = mrId
            )
        )
    }

    override fun getMrReviewInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): Result<GitMrReviewInfo?> {
        return Result(
            scmOauthService.getMrReviewInfo(
                projectName = projectName,
                url = url,
                type = type,
                token = token,
                mrId = mrId
            )
        )
    }

    override fun getMrCommitList(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long,
        page: Int,
        size: Int
    ): Result<List<GitCommit>> {
        return Result(
            scmOauthService.getMrCommitList(
                projectName = projectName,
                url = url,
                type = type,
                token = token,
                mrId = mrId,
                page = page,
                size = size
            )
        )
    }

    override fun getProjectInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?
    ): Result<RepositoryProjectInfo> {
        return Result(
            scmOauthService.getProjectInfo(
                projectName = projectName,
                url = url,
                type = type,
                token = token
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceScmOauthResourceImpl::class.java)
    }
}
