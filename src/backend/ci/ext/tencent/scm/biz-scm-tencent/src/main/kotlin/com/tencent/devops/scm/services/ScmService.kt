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
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.ScmFactory
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.config.P4Config
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.exception.GitApiException
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.utils.QualityUtils
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmService @Autowired constructor(
    private val gitConfig: GitConfig,
    private val svnConfig: SVNConfig,
    private val p4Config: P4Config,
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
        logger.info("[$projectName|$url|$type|$userName] Start to get latest revision")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = branchName,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName
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
        search: String? = null,
        page: Int,
        pageSize: Int
    ): List<String> {
        logger.info("[$projectName|$url|$type|$userName] Start to list branches")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName
            ).getBranches(search = search, page = page, pageSize = pageSize)
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
            return ScmFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = null,
                passPhrase = null,
                token = token,
                region = null,
                userName = userName
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
        logger.info("[$projectName|$url|$type|$userName] Start to check the private key and token")
        val startEpoch = System.currentTimeMillis()
        try {
            ScmFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = privateKey,
                passPhrase = passPhrase,
                token = token,
                region = region,
                userName = userName
            ).checkTokenAndPrivateKey()
        } catch (e: Throwable) {
            logger.warn(
                "Fail to check the private key " +
                    "(projectName=$projectName, type=$type, region=$region, username=$userName)",
                e
            )
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check the private key and token")
        }
        return TokenCheckResult(true, "OK")
    }

    fun checkUsernameAndPassword(
        projectName: String,
        url: String,
        type: ScmType,
        username: String,
        password: String,
        token: String,
        region: CodeSvnRegion?,
        repoUsername: String
    ): TokenCheckResult {
        logger.info("[$projectName|$url|$type|$username|$repoUsername] Start to check the username and password")
        val startEpoch = System.currentTimeMillis()
        try {
            ScmFactory.getScm(
                projectName = projectName,
                url = url,
                type = type,
                branchName = null,
                privateKey = username,
                passPhrase = password,
                token = token,
                region = region,
                userName = repoUsername
            ).checkTokenAndUsername()
        } catch (e: Throwable) {
            logger.warn(
                "Fail to check the private key " +
                    "(projectName=$projectName, type=$type, username=$username, repoUsername=$repoUsername)",
                e
            )
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check username and password")
        }
        return TokenCheckResult(true, "OK")
    }

    @SuppressWarnings("LongMethod", "NestedBlockDepth", "ThrowsCount", "LongParameterList")
    fun addWebHook(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        event: String? = null,
        hookUrl: String? = null
    ) {
        logger.info("[$projectName|$url|$type|$region|$userName|$event|$hookUrl] Start to add web hook")
        val startEpoch = System.currentTimeMillis()
        try {
            val realHookUrl = if (!hookUrl.isNullOrBlank()) {
                hookUrl
            } else {
                when (type) {
                    ScmType.CODE_GIT -> {
                        if (gitConfig.gitHookUrl.isBlank()) {
                            logger.warn("The git webhook url is not settle")
                            throw IllegalArgumentException("The git hook url is not settle")
                        }
                        gitConfig.gitHookUrl
                    }
                    ScmType.CODE_GITLAB -> {
                        if (gitConfig.gitlabHookUrl.isBlank()) {
                            logger.warn("The gitlab webhook url is not settle")
                            throw IllegalArgumentException("The gitlab webhook url is not settle")
                        }
                        gitConfig.gitlabHookUrl
                    }
                    ScmType.CODE_SVN -> {
                        if (svnConfig.svnHookUrl.isBlank()) {
                            logger.warn("The svn webhook url is not settle")
                            throw IllegalArgumentException("The svn webhook url is not settle")
                        }
                        svnConfig.svnHookUrl
                    }
                    ScmType.CODE_TGIT -> {
                        if (gitConfig.tGitHookUrl.isBlank()) {
                            logger.warn("The tgit webhook url is not settle")
                            throw IllegalArgumentException("The tgit webhook url is not settle")
                        }
                        gitConfig.tGitHookUrl
                    }
                    ScmType.CODE_P4 -> {
                        if (p4Config.p4HookUrl.isBlank()) {
                            logger.warn("The p4 webhook url is not settle")
                            throw IllegalArgumentException("The p4 webhook url is not settle")
                        }
                        p4Config.p4HookUrl
                    }
                    else -> {
                        logger.warn("Unknown repository type ($type) when add webhook")
                        throw IllegalArgumentException("Unknown repository type ($type) when add webhook")
                    }
                }
            }
            ScmFactory.getScm(
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
            )
                .addWebHook(hookUrl = realHookUrl)
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
        var statusMessage: String? = null
        try {
            with(request) {
                val scm = ScmFactory.getScm(
                    projectName = projectName,
                    url = url,
                    type = type,
                    branchName = null,
                    privateKey = privateKey,
                    passPhrase = passPhrase,
                    token = token,
                    region = region,
                    userName = "",
                    event = CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value
                )
                requestTime = System.currentTimeMillis()
                scm.addCommitCheck(commitId, state, targetUrl, context, description, block)
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
                e.message ?: I18nUtil.getCodeLanMessage(
                    messageCode = RepositoryMessageCode.GIT_TOKEN_FAIL),
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

    fun lock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    ) {
        if (type != ScmType.CODE_SVN) {
            logger.warn("repository type ($type) can not lock")
            throw RuntimeException("repository type ($type) can not lock")
        }
        val repName = SvnUtils.getSvnRepName(url)
        val subPath = SvnUtils.getSvnSubPath(url)
        val svnRegion = region ?: CodeSvnRegion.getRegion(url)

        ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = "",
            passPhrase = "",
            token = "",
            region = svnRegion,
            userName = userName
        ).lock(repName, userName, subPath)
    }

    fun unlock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    ) {
        if (type != ScmType.CODE_SVN) {
            logger.warn("repository type ($type) can not unlock")
            throw RuntimeException("repository type ($type) can not unlock")
        }
        val repName = SvnUtils.getSvnRepName(url)
        val subPath = SvnUtils.getSvnSubPath(url)
        val svnRegion = region ?: CodeSvnRegion.getRegion(url)

        ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = "",
            passPhrase = "",
            token = "",
            region = svnRegion,
            userName = userName
        ).unlock(repName, userName, subPath)
    }

    fun getMergeRequestChangeInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrChangeInfo? {
        return ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null
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
        return ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null
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
        return ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null
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
        return ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null
        ).getMrCommitList(mrId = mrId, page = page, size = size)
    }

    fun getProjectInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?
    ): GitProjectInfo? {
        return ScmFactory.getScm(
            projectName = projectName,
            url = url,
            type = type,
            branchName = null,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            userName = null
        ).getProjectInfo(projectName = projectName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmService::class.java)
    }
}
