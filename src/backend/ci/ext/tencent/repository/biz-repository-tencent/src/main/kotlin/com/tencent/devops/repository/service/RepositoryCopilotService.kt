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
package com.tencent.devops.repository.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.repository.AISummaryRateType
import com.tencent.devops.repository.config.CopilotConfig
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.CopilotSummaryDao
import com.tencent.devops.repository.enum.CopilotSummaryCreateStatus
import com.tencent.devops.repository.pojo.CodeGitCopilotSummary
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.service.api.CopilotApi
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.concurrent.Executors

@Service
class RepositoryCopilotService @Autowired constructor(
    val dslContext: DSLContext,
    val repositoryService: RepositoryService,
    val commitService: CommitService,
    val copilotConfig: CopilotConfig,
    val gitOauthService: IGitOauthService,
    val copilotSummaryDao: CopilotSummaryDao
) {

    val copilotApi = CopilotApi()

    fun createSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        refresh: Boolean
    ): CodeGitCopilotSummary {
        logger.info("start create summary|$projectId|$pipelineId|$buildId|$elementId|$refresh")
        val accessToken = getAccessToken(userId)
        val summary = if (refresh) {
            null
        } else {
            copilotSummaryDao.get(
                dslContext = dslContext,
                buildId = buildId,
                elementId = elementId
            )?.summary
        }
        // 返回已有摘要结果
        if (!summary.isNullOrBlank()) {
            return JsonUtil.to(summary, CodeGitCopilotSummary::class.java)
        }
        val (projectName, sourceSha, targetSha) = resolveSummaryParams(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId
        )
        executorService.execute {
            logger.info("async get summary|$projectName|$sourceSha...$targetSha")
            val copilotSummary = copilotApi.getSummary(
                url = "${copilotConfig.apiHost}/projects/${encodeProjectName(projectName)}/summary",
                sourceSha = sourceSha,
                targetSha = targetSha,
                accessToken = accessToken
            )?.let {
                it.projectName = projectName
                it
            } ?: throw ErrorCodeException(errorCode = RepositoryMessageCode.EMPTY_COMMIT_RECORD)
            logger.info("async save summary|$copilotSummary")
            copilotSummaryDao.create(
                dslContext = dslContext,
                buildId = buildId,
                elementId = elementId,
                summary = JsonUtil.toJson(copilotSummary, false),
                source = sourceSha,
                target = targetSha,
                projectName = projectName,
                scmCode = SCM_CODE,
                status = copilotSummary.status.toString()
            )
        }
        val runningSummary = CodeGitCopilotSummary(status = CopilotSummaryCreateStatus.RUNNING.value)
        copilotSummaryDao.create(
            dslContext = dslContext,
            buildId = buildId,
            elementId = elementId,
            summary = JsonUtil.toJson(runningSummary, false),
            source = sourceSha,
            target = targetSha,
            projectName = projectName,
            scmCode = SCM_CODE,
            status = CopilotSummaryCreateStatus.RUNNING.value.toString()
        )
        return runningSummary
    }

    fun rateSummary(
        projectName: String,
        processId: String,
        userId: String,
        type: AISummaryRateType,
        feedback: String? = null
    ) {
        val accessToken = getAccessToken(userId)
        copilotApi.rateSummary(
            url = "${copilotConfig.apiHost}/projects/${encodeProjectName(projectName)}/summary/rate",
            processId = processId,
            type = type,
            accessToken = accessToken,
            feedback = feedback
        )
    }

    private fun encodeProjectName(projectName: String) =
        URLEncoder.encode(projectName, "UTF-8")

    private fun getAccessToken(userId: String) =
        gitOauthService.getAccessToken(userId)?.accessToken ?: throw ErrorCodeException(
            errorCode = RepositoryMessageCode.NOT_AUTHORIZED_BY_OAUTH
        )

    /**
     * 提取摘要参数
     */
    private fun resolveSummaryParams(
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String
    ): Triple<String, String, String> {
        val commitRecords = commitService.list(
            buildId = buildId,
            pipelineId = pipelineId,
            elementId = elementId
        )
        if (commitRecords.isEmpty()) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.EMPTY_COMMIT_RECORD
            )
        }
        val (repoId, repoUrl) = commitRecords.first().let { it.repoId to it.url }
        val projectName = if (repoId == 0L) { // url拉取
            val (host, projectName) = GitUtils.getDomainAndRepoName(repoUrl)
            if (!copilotConfig.supportHost.contains(host)) {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.REPOSITORY_NO_SUPPORT_AI_SUMMARY
                )
            }
            projectName
        } else {
            val repository = repositoryService.getRepository(
                projectId = projectId,
                repositoryHashId = HashUtil.encodeOtherLongId(repoId),
                repoAliasName = null
            )
            // 仅支持code git
            if (repository !is CodeGitRepository) {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.REPOSITORY_NO_SUPPORT_AI_SUMMARY
                )
            }
            repository.projectName
        }
        val (sourceSha, targetSha) = commitRecords.last().commit to commitRecords.first().commit
        return Triple(projectName, sourceSha, targetSha)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryCopilotService::class.java)
        private val executorService = Executors.newFixedThreadPool(50)
        const val SCM_CODE = "TGIT"
    }
}