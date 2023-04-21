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
import com.tencent.devops.repository.utils.scm.QualityUtils
import com.tencent.devops.scm.ScmOauthFactory
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class ScmOauthService @Autowired constructor(
    private val gitConfig: GitConfig,
    private val svnConfig: SVNConfig
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

    override fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String,
        search: String?
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
            )
                .getTags(search = search)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list tags")
        }
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
        logger.info("[$projectName|$url|$type|$userName] Start to check private key and token")
        val startEpoch = System.currentTimeMillis()
        try {
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
                event = null
            )
                .checkTokenAndPrivateKey()
        } catch (e: Throwable) {
            logger.warn("CheckPrivateKeyFail|projectName=$projectName|type=$type|region=$region|username=$userName", e)
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check private key and token")
        }
        return TokenCheckResult(true, "OK")
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
        logger.info("[$projectName|$url|$type|$userName] Start to add web hook")
        val startEpoch = System.currentTimeMillis()
        try {
            val hookUrl = when (type) {
                ScmType.CODE_GIT -> {
                    gitConfig.gitHookUrl
                }
                ScmType.CODE_GITLAB -> {
                    gitConfig.gitlabHookUrl
                }
                ScmType.CODE_SVN -> {
                    svnConfig.svnHookUrl
                }
                else -> {
                    throw IllegalArgumentException("Unknown repository type ($type) when add webhook")
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
            )
                .addWebHook(hookUrl)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add web hook")
        }
    }

    override fun addCommitCheck(
        request: CommitCheckRequest
    ) {
        val startEpoch = System.currentTimeMillis()
        try {
            with(request) {
                val scm =
                    ScmOauthFactory.getScm(
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
                scm.addCommitCheck(
                    commitId = commitId,
                    state = state,
                    targetUrl = targetUrl,
                    context = context,
                    description = description,
                    block = block,
                    targetBranch = targetBranch
                )
                if (mrRequestId != null) {
                    if (reportData.second.isEmpty()) return
                    val comment = QualityUtils.getQualityReport(
                        titleData = reportData.first,
                        resultData = reportData.second
                    )
                    scm.addMRComment(mrRequestId!!, comment)
                }
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add commit check")
        }
    }

    override fun getMergeRequestChangeInfo(
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

    override fun getMrInfo(
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

    override fun getMrReviewInfo(
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

    override fun getMrCommitList(
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

    override fun getProjectInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?
    ): GitProjectInfo? {
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
        ).getProjectInfo(
            projectName = projectName
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmOauthService::class.java)
    }
}
