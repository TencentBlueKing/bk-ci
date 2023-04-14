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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.BUILD_TASK_NOT_EXIST
import com.tencent.devops.stream.constant.StreamMessageCode.PIPELINE_NOT_FOUND_OR_DELETED
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class StreamTriggerService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamGitConfig: StreamGitConfig
) {

    @Value("\${rtx.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    private val channelCode = ChannelCode.GIT

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerService::class.java)
        private const val ymlVersion = "v2.0"
        const val BK_REPO_GIT_WEBHOOK_MR_IID = "BK_CI_REPO_GIT_WEBHOOK_MR_IID"
    }

    fun retry(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        taskId: String?,
        failedContainer: Boolean? = false
    ): BuildId {
        logger.info("StreamTriggerService|retry|gitProjectId|$gitProjectId|pipelineId|$pipelineId|buildId|$buildId")
        val pipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId) ?: throw CustomException(
                Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(PIPELINE_NOT_FOUND_OR_DELETED, I18nUtil.getLanguage(userId))
            )
        val gitEventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId)
            ?: throw CustomException(
                Response.Status.NOT_FOUND,
                MessageUtil.getMessageByLocale(BUILD_TASK_NOT_EXIST, I18nUtil.getLanguage(userId))
            )
        val newBuildId = client.get(ServiceBuildResource::class).retry(
            userId = userId,
            projectId = GitCommonUtils.getCiProjectId(pipeline.gitProjectId, streamGitConfig.getScmType()),
            pipelineId = pipeline.pipelineId,
            buildId = buildId,
            taskId = taskId,
            channelCode = channelCode,
            failedContainer = failedContainer
        ).data!!

        gitRequestEventBuildDao.retryUpdate(
            dslContext = dslContext,
            gitBuildId = gitEventBuild.id
        )
        return newBuildId
    }

    fun manualShutdown(userId: String, gitProjectId: Long, pipelineId: String, buildId: String): Boolean {
        logger.info(
            "StreamTriggerService|manualShutdown" +
                "|gitProjectId|$gitProjectId|pipelineId|$pipelineId|buildId|$buildId"
        )
        val pipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId) ?: throw CustomException(
                Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(PIPELINE_NOT_FOUND_OR_DELETED, I18nUtil.getLanguage(userId))
            )

        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = GitCommonUtils.getCiProjectId(pipeline.gitProjectId, streamGitConfig.getScmType()),
            pipelineId = pipeline.pipelineId,
            buildId = buildId,
            channelCode = channelCode
        ).data!!
    }
}
