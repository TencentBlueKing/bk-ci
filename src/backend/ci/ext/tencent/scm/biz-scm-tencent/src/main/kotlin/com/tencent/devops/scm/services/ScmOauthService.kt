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

package com.tencent.devops.scm.services

import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.scm.ScmFactory
import com.tencent.devops.scm.ScmOauthFactory
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.exception.GitApiException
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.RepositoryProjectInfo
import com.tencent.devops.scm.utils.QualityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmOauthService @Autowired constructor(
    private val gitConfig: GitConfig,
    private val svnConfig: SVNConfig,
    private val scmMonitorService: ScmMonitorService
) {

    fun getLatestRevision(
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
        logger.info("[$projectName|$url|$type|$branchName|$userName] Start to get the latest oauth revision")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmOauthFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = branchName,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName,
                event = null
            ).getLatestRevision()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the latest revision")
        }
    }

    fun listBranches(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?,
        search: String? = null
    ): List<String> {
        logger.info("[$projectName|$url|$type|$userName] Start to list the branches")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmOauthFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName,
                event = null
            ).getBranches(search = search)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list branches")
        }
    }

    fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String,
        search: String? = null
    ): List<String> {
        logger.info("[$projectName|$url|$type|$userName] Start to list tags")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmOauthFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = null,
                passPhrase = null,
                token = token,
                region = null,
                userName = userName,
                event = null
            ).getTags(search = search)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list tags")
        }
    }

    fun checkPrivateKeyAndToken(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): TokenCheckResult {
        logger.info("[$projectName|$url|$type|$userName] Start to check private key and token")
        val startEpoch = System.currentTimeMillis()
        try {
            ScmOauthFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, userName, null)
                .checkTokenAndPrivateKey()
        } catch (e: Throwable) {
            logger.warn(
                "Fail to check the private key " +
                    "(projectName=$projectName, type=$type, region=$region, username=$userName)",
                e
            )
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check private key and token")
        }
        return TokenCheckResult(true, "OK")
    }

    fun addWebHook(
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
        logger.info("[$projectName|$url|$type|$userName] Start to add web hook")
        val startEpoch = System.currentTimeMillis()
        try {
            val hookUrl = when (type) {
                ScmType.CODE_GIT -> {
                    if (gitConfig.gitHookUrl.isBlank()) {
                        logger.warn("The git webhook url is not settle")
                        throw RuntimeException("The git hook url is not settle")
                    }
                    gitConfig.gitHookUrl
                }
                ScmType.CODE_GITLAB -> {
                    if (gitConfig.gitlabHookUrl.isBlank()) {
                        logger.warn("The gitlab webhook url is not settle")
                        throw RuntimeException("The gitlab webhook url is not settle")
                    }
                    gitConfig.gitlabHookUrl
                }
                ScmType.CODE_SVN -> {
                    if (svnConfig.svnHookUrl.isBlank()) {
                        logger.warn("The svn webhook url is not settle")
                        throw RuntimeException("The svn webhook url is not settle")
                    }
                    svnConfig.svnHookUrl
                }
                else -> {
                    logger.warn("Unknown repository type ($type) when add webhook")
                    throw RuntimeException("Unknown repository type ($type) when add webhook")
                }
            }
            ScmOauthFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName,
                event = event
            ).addWebHook(hookUrl)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add web hook")
        }
    }

    @BkTimed
    @Suppress("NestedBlockDepth")
    fun addCommitCheck(
        request: CommitCheckRequest
    ) {
        val startEpoch = System.currentTimeMillis()
        var requestTime = System.currentTimeMillis()
        var responseTime = System.currentTimeMillis()
        var statusCode: Int = HTTP_200
        var statusMessage: String? = "OK"
        try {
            with(request) {
                val scm = ScmOauthFactory.getScm(
                    projectName = projectName,
                    url = url,
                    type = type,
                    branchName = null,
                    privateKey = privateKey,
                    passPhrase = passPhrase,
                    token = token,
                    region = region,
                    userName = "",
                    event = ""
                )
                requestTime = System.currentTimeMillis()
                scm.addCommitCheck(
                    commitId = commitId,
                    state = state,
                    targetUrl = targetUrl,
                    context = context,
                    description = description,
                    block = block
                )
                responseTime = System.currentTimeMillis()
                if (mrRequestId != null) {
                    if (reportData.second.isEmpty()) return
                    val comment = QualityUtils.getQualityReport(reportData.first, reportData.second)
                    scm.addMRComment(mrRequestId!!, comment)
                }
            }
        } catch (e: GitApiException) {
            responseTime = System.currentTimeMillis()
            statusCode = e.code
            statusMessage = e.message
            throw ScmException(
                e.message ?: MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GIT_TOKEN_FAIL),
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

    fun getMergeRequestChangeInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrChangeInfo? {
        return ScmOauthFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null,
            event = null
        )
            .getMergeRequestChangeInfo(mrId = mrId)
    }

    fun getMrInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrInfo? {
        return ScmOauthFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null,
            event = null
        )
            .getMrInfo(mrId = mrId)
    }

    fun getMrReviewInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrReviewInfo? {
        return ScmOauthFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null,
            event = null
        )
            .getMrReviewInfo(mrId = mrId)
    }

    fun getMrCommitList(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long,
        page: Int,
        size: Int
    ): List<GitCommit> {
        return ScmOauthFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null,
            event = null
        ).getMrCommitList(mrId = mrId, page = page, size = size)
    }

    fun getProjectInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?
    ): RepositoryProjectInfo {
        return ScmOauthFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null,
            event = null
        ).getProjectInfo(projectName = projectName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmOauthService::class.java)
    }
}
