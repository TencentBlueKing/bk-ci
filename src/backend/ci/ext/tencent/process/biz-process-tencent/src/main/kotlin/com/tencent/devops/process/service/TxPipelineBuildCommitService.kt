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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.lambda.LambdaMessageCode
import com.tencent.devops.process.config.ProcessKafkaTopicConfig
import com.tencent.devops.process.pojo.DataPlatBuildCommits
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import com.tencent.devops.process.service.builds.PipelineBuildCommitService
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Primary
class TxPipelineBuildCommitService @Autowired constructor(
    private val kafkaClient: KafkaClient,
    private val processKafkaTopicConfig: ProcessKafkaTopicConfig
) : PipelineBuildCommitService() {

    override fun create(
        projectId: String,
        pipelineId: String,
        buildId: String,
        matcher: ScmWebhookMatcher,
        repo: Repository
    ) {
        try {
            var page = 1
            val size = 200
            while (true) {
                val webhookCommitList = matcher.getWebhookCommitList(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repository = repo,
                    page = page,
                    size = size
                )
                if (webhookCommitList.isEmpty()) {
                    logger.info("the pipeline build commit is empty|$projectId|$pipelineId|$buildId|$repo")
                    return
                }
                val buildCommits = webhookCommitList.map {
                    with(it) {
                        DataPlatBuildCommits(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            commitId = commitId,
                            authorName = authorName,
                            message = message,
                            repoType = matcher.getCodeType().name,
                            commitTime = commitTime.format(dateTimeFormatter),
                            createTime = LocalDateTime.now().format(dateTimeFormatter),
                            mrId = mrId ?: "",
                            url = repo.url,
                            eventType = eventType,
                            channel = ChannelCode.BS.name,
                            action = action
                        )
                    }
                }
                logger.info(
                    "start send build commit[${buildCommits.first()}...${buildCommits.last()}]|" +
                            "${buildCommits.size}"
                )
                checkParamBlank(processKafkaTopicConfig.buildCommitsTopic, "buildCommitsTopic")
                kafkaClient.send(processKafkaTopicConfig.buildCommitsTopic!!, JsonUtil.toJson(buildCommits))
                if (webhookCommitList.size < size) break
                page++
            }
        } catch (ignored: Throwable) {
            logger.info("save build info err | err is $ignored")
        }
    }

    override fun saveCommits(commits: List<PipelineBuildCommit>) {
        try {
            val buildCommits = commits.map { buildCommit ->
                with(buildCommit) {
                    DataPlatBuildCommits(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        commitId = commitId,
                        authorName = authorName,
                        message = message,
                        repoType = repoType,
                        commitTime = commitTime.format(dateTimeFormatter),
                        createTime = LocalDateTime.now().format(dateTimeFormatter),
                        mrId = mrId ?: "",
                        url = url,
                        eventType = eventType,
                        channel = channel,
                        action = action ?: ""
                    )
                }
            }
            if (buildCommits.isEmpty()) {
                logger.info("the pipeline build commit is empty")
                return
            }
            logger.info(
                "start send build commit[${buildCommits.first()}...${buildCommits.last()}]|" +
                        "${buildCommits.size}"
            )
            checkParamBlank(processKafkaTopicConfig.buildCommitsTopic, "buildCommitsTopic")
            kafkaClient.send(processKafkaTopicConfig.buildCommitsTopic!!, JsonUtil.toJson(buildCommits))
        } catch (ignored: Exception) {
            logger.info("save build info err | err is $ignored")
        }
    }

    private fun checkParamBlank(param: String?, message: String): String {
        if (param.isNullOrBlank()) {
            throw ParamBlankException(
                I18nUtil.getCodeLanMessage(
                    messageCode = LambdaMessageCode.STARTUP_CONFIGURATION_MISSING,
                    params = arrayOf(message)
                )
            )
        }
        return param
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineBuildCommitService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
