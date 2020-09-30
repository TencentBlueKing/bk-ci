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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service.scm

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.repository.utils.scm.QualityUtils
import com.tencent.devops.scm.ScmFactory
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmService @Autowired constructor(
    private val svnConfig: SVNConfig,
    private val gitConfig: GitConfig
) : IScmService {

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

    override fun listBranches(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
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
            )
                .getBranches()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list branches")
        }
    }

    override fun deleteBranch(
        projectName: String,
        url: String,
        type: ScmType,
        branch: String,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ) {
        logger.info("[$projectName|$url|$type|$userName] Start to list branches")
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
            )
                .deleteBranch(branch)
        } catch (ignored: MismatchedInputException) {
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to delete branches")
        }
    }

    override fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String
    ): List<String> {
        logger.info("[$projectName|$url|$type|$token|$userName] Start to list tags")
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
            ).getTags()
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
        logger.info("[$projectName|$url|$type|$token|$userName] Start to check the private key and token")
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
            )
                .checkTokenAndPrivateKey()
        } catch (e: Throwable) {
            logger.warn(
                "Fail to check the private key (projectName=$projectName, type=$type, privateKey=$privateKey, passPhrase=$passPhrase, token=$token, region=$region, username=$userName",
                e
            )
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check the private key and token")
        }
        return TokenCheckResult(true, "OK")
    }

    override fun checkUsernameAndPassword(
        projectName: String,
        url: String,
        type: ScmType,
        username: String,
        password: String,
        token: String,
        region: CodeSvnRegion?,
        repoUsername: String
    ): TokenCheckResult {
        logger.info("[$projectName|$url|$type|$username|$password|$token|$region|$repoUsername] Start to check the username and password")
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
            )
                .checkTokenAndUsername()
        } catch (e: Throwable) {
            logger.warn(
                "Fail to check the private key (projectName=$projectName, type=$type, username=$username, token=$token, region=$region, repoUsername=$repoUsername",
                e
            )
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check username and password")
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
        event: String?,
        hookUrl: String?
    ) {
        logger.info("[$projectName|$url|$type|$token|$region|$userName|$event|$hookUrl] Start to add web hook")
        val startEpoch = System.currentTimeMillis()
        try {
            val realHookUrl = hookUrl ?: when (type) {
                ScmType.CODE_GIT -> {
                    gitConfig.gitHookUrl
                }
                ScmType.CODE_GITLAB -> {
                    gitConfig.gitlabHookUrl
                }
                ScmType.CODE_SVN -> {
                    svnConfig.svnHookUrl
                }
                ScmType.CODE_TGIT -> {
                    gitConfig.tGitHookUrl
                }
                else -> {
                    logger.warn("Unknown repository type ($type) when add webhook")
                    throw RuntimeException("Unknown repository type ($type) when add webhook")
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
                .addWebHook(realHookUrl)
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
                scm.addCommitCheck(
                    commitId = commitId,
                    state = state,
                    targetUrl = targetUrl,
                    context = context,
                    description = description,
                    block = block
                )
                if (mrRequestId != null) {
                    if (reportData.second.isEmpty()) return
                    val comment = QualityUtils.getQualityReport(reportData.first, reportData.second)
                    scm.addMRComment(mrRequestId!!, comment)
                }
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add commit check")
        }
    }

    override fun lock(
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
        )
            .lock(repoName = repName, applicant = userName, subpath = subPath)
    }

    override fun unlock(
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
        )
            .unlock(repoName = repName, applicant = userName, subpath = subPath)
    }

    override fun createBranch(
        projectName: String,
        url: String,
        type: ScmType,
        branch: String,
        ref: String,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ) {
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
        )
            .createBranch(branch = branch, ref = ref)
    }

    override fun listCommits(
        projectName: String,
        url: String,
        type: ScmType,
        branch: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        all: Boolean,
        page: Int,
        size: Int
    ): List<GitCommit> {
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
        )
            .getCommits(branch = branch, all = all, page = page, size = size)
    }

    override fun getCommitDiff(
        projectName: String,
        url: String,
        type: ScmType,
        sha: String,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): List<GitDiff> {
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
        )
            .getCommitDiff(sha)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmService::class.java)
    }
}