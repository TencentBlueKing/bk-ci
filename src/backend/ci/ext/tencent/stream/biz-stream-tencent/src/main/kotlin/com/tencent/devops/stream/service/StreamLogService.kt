/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.PIPELINE_NOT_FOUND_OR_DELETED
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.util.GitCommonUtils
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StreamLogService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamLogService::class.java)
    }

    @Value("\${gateway.url}")
    private lateinit var gatewayUrl: String

    fun getInitLogs(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): QueryLogs {
        logger.info("StreamLogService|getInitLogs|gitProjectId|$gitProjectId|pipelineId|$pipelineId|build|$buildId")
        val target = resolveLogQueryTarget(gitProjectId, pipelineId, buildId)
        return client.get(ServiceLogResource::class).getInitLogs(
            userId = userId,
            projectId = target.projectId,
            pipelineId = target.pipelineId,
            buildId = buildId,
            tag = tag,
            containerHashId = jobId,
            executeCount = executeCount,
            debug = debug,
            subTag = subTag,
            jobId = null,
            stepId = null,
            checkPermissionFlag = false
        ).data!!
    }

    fun getAfterLogs(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): QueryLogs {
        logger.info("StreamLogService|getAfterLogs|gitProjectId|$gitProjectId|pipelineId|$pipelineId|build|$buildId")
        val target = resolveLogQueryTarget(gitProjectId, pipelineId, buildId)
        return client.get(ServiceLogResource::class).getAfterLogs(
            userId = userId,
            projectId = target.projectId,
            pipelineId = target.pipelineId,
            buildId = buildId,
            start = start,
            tag = tag,
            containerHashId = jobId,
            executeCount = executeCount,
            debug = debug,
            subTag = subTag,
            jobId = null,
            stepId = null,
            checkPermissionFlag = false
        ).data!!
    }

    fun getLogMode(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ): QueryLogStatus {
        logger.info("StreamLogService|getLogMode|gitProjectId|$gitProjectId|pipelineId|$pipelineId|build|$buildId")
        val target = resolveLogQueryTarget(gitProjectId, pipelineId, buildId)
        return client.get(ServiceLogResource::class).getLogMode(
            userId = userId,
            projectId = target.projectId,
            pipelineId = target.pipelineId,
            buildId = buildId,
            tag = tag,
            executeCount = executeCount,
            stepId = null
        ).data!!
    }

    fun downloadLogs(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        fileName: String? = null
    ): Response {
        logger.info("StreamLogService|downloadLogs|gitProjectId|$gitProjectId|pipelineId|$pipelineId|build|$buildId")
        val target = resolveLogQueryTarget(gitProjectId, pipelineId, buildId)
        val path = StringBuilder("http://$gatewayUrl/log/api/service/logs/")
        path.append(target.projectId)
        path.append("/${target.pipelineId}/$buildId/download?executeCount=${executeCount ?: 1}")

        if (!tag.isNullOrBlank()) path.append("&tag=$tag")
        if (!jobId.isNullOrBlank()) path.append("&jobId=$jobId")
        if (!fileName.isNullOrBlank()) path.append("&fileName=$fileName")

        val headers = mutableMapOf(AUTH_HEADER_USER_ID to userId)

        val devopsToken = EnvironmentUtil.gatewayDevopsToken()
        if (devopsToken != null) {
            headers["X-DEVOPS-TOKEN"] = devopsToken
        }

        val response = OkhttpUtils.doLongGet(path.toString(), headers)
        val downloadName = fileName?.ifBlank { null } ?: "${target.pipelineId}-$buildId-log.txt"
        return Response
            .ok(response.body!!.byteStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $downloadName")
            .header("Cache-Control", "no-cache")
            .header("X-DEVOPS-PROJECT-ID", target.projectId)
            .build()
    }

    /**
     * 用构建记录反查流水线真实归属，禁止透传前端 projectId 去打 log。
     */
    private fun resolveLogQueryTarget(
        gitProjectId: Long,
        pipelineId: String,
        buildId: String
    ): StreamLogQueryTarget {
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId)
            ?: throw pipelineNotFound()
        if (eventBuild.pipelineId.isNullOrBlank() || eventBuild.pipelineId != pipelineId) {
            throw pipelineNotFound()
        }
        val ownerPipeline = gitPipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = null,
            pipelineIds = listOf(eventBuild.pipelineId)
        ).firstOrNull() ?: throw pipelineNotFound()

        if (!StreamLogProjectResolver.canViewFromGitProject(
                viewingGitProjectId = gitProjectId,
                ownerGitProjectId = ownerPipeline.gitProjectId,
                eventGitProjectId = eventBuild.gitProjectId,
                sourceGitProjectId = eventBuild.sourceGitProjectId
            )
        ) {
            throw pipelineNotFound()
        }

        val logProjectId = streamBasicSettingService.getStreamConf(ownerPipeline.gitProjectId)?.projectCode
            ?: GitCommonUtils.getCiProjectId(ownerPipeline.gitProjectId, streamGitConfig.getScmType())
        return StreamLogQueryTarget(
            projectId = logProjectId,
            pipelineId = ownerPipeline.pipelineId
        )
    }

    private fun pipelineNotFound(): CustomException {
        return CustomException(
            Response.Status.FORBIDDEN,
            I18nUtil.getCodeLanMessage(PIPELINE_NOT_FOUND_OR_DELETED)
        )
    }
}
