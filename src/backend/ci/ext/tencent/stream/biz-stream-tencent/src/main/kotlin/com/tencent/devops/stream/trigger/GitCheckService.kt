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

package com.tencent.devops.stream.trigger

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.repository.api.ServiceRepositoryGitCheckResource
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.pojo.ExecuteSource
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.trigger.pojo.StreamGitProjectCache
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCheckService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val tokenService: StreamGitTokenService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCheckService::class.java)
    }

    fun pushCommitCheck(
        streamGitProjectInfo: StreamGitProjectCache,
        commitId: String,
        description: String,
        mergeRequestId: Long?,
        projectId: Long,
        buildId: String,
        userId: String,
        status: GitCICommitCheckState,
        context: String,
        targetUrl: String,
        pipelineId: String,
        block: Boolean,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>> = Pair(listOf(), mutableMapOf())
    ) {
        pushCommitCheck(
            event = GitCommitCheckEvent(
                projectId = GitCommonUtils.getCiProjectId(projectId),
                pipelineId = pipelineId,
                buildId = buildId,
                repositoryConfig = RepositoryConfig(
                    repositoryHashId = streamGitProjectInfo.gitProjectId.toString(),
                    repositoryName = null,
                    repositoryType = RepositoryType.ID
                ),
                commitId = commitId,
                state = status.value,
                block = block,
                status = status.value,
                mergeRequestId = mergeRequestId,
                source = "",
                userId = userId
            ),
            streamGitProjectInfo = streamGitProjectInfo,
            context = context,
            description = description,
            targetUrl = targetUrl,
            reportData = reportData
        )
    }

    // todo: 后期修改1、增加filePath字段 2、修改RepositoryConfig增加兼容Stream
    private fun pushCommitCheck(
        event: GitCommitCheckEvent,
        streamGitProjectInfo: StreamGitProjectCache,
        context: String,
        targetUrl: String,
        description: String,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>
    ) {
        with(event) {
            logger.info(
                "Code web hook add commit check [projectId=$projectId, pipelineId=$pipelineId, buildId=$buildId, " +
                    "repoHashId=$repositoryConfig, commitId=$commitId, state=$state, block=$block]"
            )
            val buildHistoryResult = client.get(ServiceBuildResource::class).getBuildVars(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.GIT
            )
            if (buildHistoryResult.isNotOk() || buildHistoryResult.data == null) {
                logger.warn("Process instance($buildId) not exist: ${buildHistoryResult.message}")
                return
            }
            val buildInfo = buildHistoryResult.data!!

            val variables = buildInfo.variables
            if (variables.isEmpty()) {
                logger.warn("Process instance($buildId) variables is empty")
                return
            }

            val buildNum = variables[PIPELINE_BUILD_NUM]

            if (buildNum == null) {
                logger.warn("Build($buildId) number is null")
                return
            }

            tryAddCommitCheck(
                context = context,
                event = event,
                targetUrl = targetUrl,
                description = description,
                streamGitProjectInfo = streamGitProjectInfo,
                buildNum = buildNum,
                reportData = reportData
            )
        }
    }

    @Suppress("NestedBlockDepth")
    private fun GitCommitCheckEvent.tryAddCommitCheck(
        context: String,
        event: GitCommitCheckEvent,
        targetUrl: String,
        description: String,
        streamGitProjectInfo: StreamGitProjectCache,
        buildNum: String,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>
    ) {
        val gitCheckClient = client.get(ServiceRepositoryGitCheckResource::class)

        while (true) {
            val lockKey = "code_git_commit_check_lock_$pipelineId"
            val redisLock = RedisLock(redisOperation, lockKey, 60)

            redisLock.use {
                if (!redisLock.tryLock()) {
                    logger.info("Code web hook commit check try lock($lockKey) fail")
                    Thread.sleep(100)
                    return@use
                }

                val record = gitCheckClient.getGitCheck(
                    pipelineId = pipelineId,
                    repositoryConfig = repositoryConfig,
                    commitId = commitId,
                    context = context
                ).data

                if (record == null) {
                    addCommitCheck(
                        event = event,
                        targetUrl = targetUrl,
                        // 兼容旧数据，如果结束是发送还是空记录肯定是旧数据
                        context = context,
                        description = description,
                        streamGitProjectInfo = streamGitProjectInfo
                    )
                    gitCheckClient.createGitCheck(
                        gitCheck = RepositoryGitCheck(
                            gitCheckId = -1,
                            pipelineId = pipelineId,
                            buildNumber = buildNum.toInt(),
                            repositoryId = streamGitProjectInfo.gitProjectId.toString(),
                            repositoryName = getProjectName(streamGitProjectInfo),
                            commitId = commitId,
                            context = context,
                            source = ExecuteSource.STREAM
                        )
                    )
                } else {
                    if (buildNum.toInt() >= record.buildNumber) {
                        addCommitCheck(
                            event = event,
                            targetUrl = targetUrl,
                            context = record.context,
                            description = description,
                            streamGitProjectInfo = streamGitProjectInfo,
                            reportData = reportData
                        )
                        gitCheckClient.updateGitCheck(
                            gitCheckId = record.gitCheckId,
                            buildNumber = buildNum.toInt()
                        )
                    } else {
                        logger.info("Code web hook commit check has bigger build number(${record.buildNumber})")
                    }
                }
                return
            }
        }
    }

    private fun addCommitCheck(
        event: GitCommitCheckEvent,
        streamGitProjectInfo: StreamGitProjectCache,
        targetUrl: String,
        context: String,
        description: String,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>> = Pair(listOf(), mutableMapOf())
    ) {
        with(event) {
            logger.info("Project($$projectId) add git commit($commitId) commit check.")

            val gitProjectId = streamGitProjectInfo.gitProjectId
            val token = tokenService.getToken(gitProjectId)
            val request = CommitCheckRequest(
                projectName = gitProjectId.toString(),
                url = streamGitProjectInfo.gitHttpUrl,
                type = ScmType.CODE_GIT,
                privateKey = null,
                passPhrase = null,
                token = token,
                region = null,
                commitId = commitId,
                state = status,
                targetUrl = targetUrl,
                context = context,
                description = description,
                block = block,
                mrRequestId = mergeRequestId,
                reportData = reportData
            )
            client.get(ServiceScmOauthResource::class).addCommitCheck(request)
        }
    }

    private fun getProjectName(conf: StreamGitProjectCache): String {
        return try {
            GitCommonUtils.getRepoName(
                httpUrl = conf.gitHttpUrl,
                name = conf.name
            )
        } catch (e: java.lang.Exception) {
            conf.name
        }
    }
}
