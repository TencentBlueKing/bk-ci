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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.CopilotSummaryDao
import com.tencent.devops.repository.enums.CopilotSummaryCreateStatus
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.TencentGitServiceImpl
import com.tencent.devops.scm.api.ServiceCopilotResource
import com.tencent.devops.scm.enums.AISummaryRateType
import com.tencent.devops.scm.pojo.CodeGitCopilotSummary
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LongParameterList", "LongMethod")
class RepositoryCopilotService @Autowired constructor(
    val client: Client,
    val dslContext: DSLContext,
    val commitService: CommitService,
    val gitOauthService: IGitOauthService,
    val copilotSummaryDao: CopilotSummaryDao,
    val redisOperation: RedisOperation,
    val gitService: TencentGitServiceImpl
) {

    fun createSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        accessToken: String? = null,
        update: Boolean = true
    ): CodeGitCopilotSummary {
        val redisLock = RedisLock(redisOperation, getLockKey(projectId, buildId, elementId), EXPIRED_TIME_IN_SECONDS)
        return redisLock.use {
            redisLock.lock()
            generateSummary(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                elementId = elementId,
                accessToken = accessToken,
                update = update
            )
        }
    }

    fun getSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String
    ): CodeGitCopilotSummary {
        // 引导用户进行oauth
        val accessToken = getAccessToken(userId)
        // 没有生成摘要，则先生成
        val record = copilotSummaryDao.get(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            elementId = elementId
        )
        return when {
            record == null ->
                createSummary(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    elementId = elementId,
                    accessToken = accessToken,
                    update = false
                )

            CopilotSummaryCreateStatus.isFinal(record.status) ->
                JsonUtil.to(record.summary, CodeGitCopilotSummary::class.java)

            else -> {
                val summary = JsonUtil.to(record.summary, CodeGitCopilotSummary::class.java)
                val copilotSummary = client.getScm(ServiceCopilotResource::class).getSummary(
                    projectName = record.projectName,
                    taskId = summary.id!!.toString(),
                    token = accessToken
                ).data ?: throw ErrorCodeException(errorCode = RepositoryMessageCode.EMPTY_COMMIT_RECORD)
                // 更新processId
                copilotSummary.processId = summary.processId
                copilotSummary.projectName = summary.projectName
                if (CopilotSummaryCreateStatus.isFinal(copilotSummary.status)) {
                    // 成功获取摘要
                    logger.info("success get summary|$projectId|$buildId|$elementId")
                    copilotSummaryDao.update(
                        dslContext = dslContext,
                        projectId = projectId,
                        buildId = buildId,
                        elementId = elementId,
                        summary = JsonUtil.toJson(copilotSummary, false),
                        status = copilotSummary.status
                    )
                }
                copilotSummary
            }
        }
    }

    fun rateSummary(
        projectName: String,
        processId: String,
        userId: String,
        type: AISummaryRateType,
        feedback: String? = null
    ) {
        val accessToken = getAccessToken(userId)
        client.getScm(ServiceCopilotResource::class).rateSummary(
            projectName = projectName,
            processId = processId,
            type = type,
            token = accessToken,
            feedback = feedback
        )
    }

    private fun getAccessToken(userId: String) = gitOauthService.getAccessToken(
        userId = userId
    )?.accessToken ?: throw ErrorCodeException(
        errorCode = RepositoryMessageCode.NOT_AUTHORIZED_BY_OAUTH
    )

    /**
     * 提取摘要参数
     */
    private fun resolveSummaryParams(
        pipelineId: String,
        buildId: String,
        elementId: String,
        accessToken: String
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
        val (type, repoUrl) = commitRecords.first().let { it.type to it.url }
        if (type != ScmType.parse(ScmType.CODE_GIT)) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.REPOSITORY_NO_SUPPORT_AI_SUMMARY
            )
        }
        val projectName = GitUtils.getDomainAndRepoName(repoUrl).second
        val firstCommit = commitRecords.first().commit
        // 仅一条变更记录的话，需要查询目标记录的父提交点
        val (sourceSha, targetSha) = if (commitRecords.size == 1) {
            val target = try {
                gitService.getRepoRecentCommitInfo(
                    repoName = projectName,
                    sha = firstCommit,
                    token = accessToken,
                    tokenType = TokenTypeEnum.OAUTH
                ).data?.parentIds?.firstOrNull()
            } catch (ignored: Exception) {
                logger.warn("get source commit info failed", ignored)
                null
            } ?: firstCommit
            firstCommit to target
        } else {
            firstCommit to commitRecords.last().commit
        }
        return Triple(projectName, sourceSha, targetSha)
    }

    private fun getLockKey(
        projectId: String,
        buildId: String,
        elementId: String
    ) = "scm:copilot:summary:$projectId:$buildId:$elementId"

    private fun generateSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        accessToken: String? = null,
        update: Boolean
    ): CodeGitCopilotSummary {
        logger.info("start generate summary|$projectId|$pipelineId|$buildId|$elementId")
        val token = accessToken ?: getAccessToken(userId)
        val (projectName, sourceSha, targetSha) = resolveSummaryParams(
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId,
            accessToken = token
        )
        logger.info("async get summary|$projectName|$sourceSha...$targetSha")
        val copilotSummary = client.getScm(ServiceCopilotResource::class).createSummary(
            projectName = projectName,
            source = sourceSha,
            target = targetSha,
            token = token
        ).data ?: throw ErrorCodeException(errorCode = RepositoryMessageCode.EMPTY_COMMIT_RECORD)
        copilotSummary.projectName = projectName
        if (update) {
            copilotSummaryDao.update(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                elementId = elementId,
                summary = JsonUtil.toJson(copilotSummary, false),
                status = copilotSummary.status
            )
        } else {
            copilotSummaryDao.create(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                elementId = elementId,
                summary = JsonUtil.toJson(copilotSummary, false),
                source = sourceSha,
                target = targetSha,
                projectName = projectName,
                scmType = COPILOT_SCM_TYPE,
                status = copilotSummary.status
            )
        }
        return copilotSummary
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryCopilotService::class.java)
        // 现阶段仅支持内网工蜂GIT仓库
        const val COPILOT_SCM_TYPE = "CODE_GIT"
        private const val EXPIRED_TIME_IN_SECONDS = 30L
    }
}