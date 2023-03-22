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

package com.tencent.devops.stream.v1.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.stream.constant.StreamCode.BK_BUILD_TASK_NOT_EXIST
import com.tencent.devops.stream.constant.StreamCode.BK_PIPELINE_NOT_EXIST_OR_DELETED
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.v1.components.V1YamlBuild
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.pojo.V1GitProjectPipeline
import com.tencent.devops.stream.v1.pojo.V1GitRepositoryConf
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import com.tencent.devops.stream.v1.utils.V1GitCIPipelineUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class V1GitCIBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitPipelineResourceDao: V1GitPipelineResourceDao,
    private val v1GitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val yamlBuild: V1YamlBuild
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1GitCIBuildService::class.java)
        const val BK_REPO_GIT_WEBHOOK_MR_IID = "BK_CI_REPO_GIT_WEBHOOK_MR_IID"
    }

    private val channelCode = ChannelCode.GIT

    fun retry(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        taskId: String?
    ): BuildId {
        logger.info("retry pipeline, gitProjectId: $gitProjectId, pipelineId: $pipelineId, buildId: $buildId")
        val pipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId) ?: throw CustomException(
                Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PIPELINE_NOT_EXIST_OR_DELETED,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        val gitEventBuild = v1GitRequestEventBuildDao.getByBuildId(dslContext, buildId)
            ?: throw CustomException(Response.Status.NOT_FOUND,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_BUILD_TASK_NOT_EXIST,
                    language = I18nUtil.getLanguage(userId)
                ))
        val newBuildId = client.get(ServiceBuildResource::class).retry(
            userId = userId,
            projectId = V1GitCIPipelineUtils.genGitProjectCode(pipeline.gitProjectId),
            pipelineId = pipeline.pipelineId,
            buildId = buildId,
            taskId = taskId,
            channelCode = channelCode
        ).data!!

        v1GitRequestEventBuildDao.retryUpdate(
            dslContext = dslContext,
            gitBuildId = gitEventBuild.id
        )
        return newBuildId
    }

    fun manualShutdown(userId: String, gitProjectId: Long, pipelineId: String, buildId: String): Boolean {
        logger.info("manualShutdown, gitProjectId: $gitProjectId, pipelineId: $pipelineId, buildId: $buildId")
        val pipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId) ?: throw CustomException(
                Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PIPELINE_NOT_EXIST_OR_DELETED,
                    language = I18nUtil.getLanguage(userId)
                )
            )

        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = V1GitCIPipelineUtils.genGitProjectCode(pipeline.gitProjectId),
            pipelineId = pipeline.pipelineId,
            buildId = buildId,
            channelCode = channelCode
        ).data!!
    }

    fun startBuild(
        pipeline: V1GitProjectPipeline,
        event: V1GitRequestEvent,
        gitProjectConf: V1GitRepositoryConf,
        model: Model,
        gitBuildId: Long
    ): BuildId? {
        val realPipeline = StreamTriggerPipeline(
            gitProjectId = pipeline.gitProjectId.toString(),
            pipelineId = pipeline.pipelineId,
            filePath = pipeline.filePath,
            displayName = pipeline.displayName,
            enabled = pipeline.enabled,
            creator = pipeline.creator
        )
        return yamlBuild.startBuild(realPipeline, event, gitProjectConf, model, gitBuildId)
    }
}
