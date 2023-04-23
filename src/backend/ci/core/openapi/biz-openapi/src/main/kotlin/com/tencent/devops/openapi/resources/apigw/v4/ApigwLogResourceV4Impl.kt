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

package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.QueryLogLineNum
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwLogResourceV4
import com.tencent.devops.openapi.service.IndexService
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@RestResource
class ApigwLogResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val indexService: IndexService
) : ApigwLogResourceV4 {

    @Value("\${devopsGateway.api:#{null}}")
    private val gatewayUrl: String? = ""

    override fun getInitLogs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        debug: Boolean?,
        elementId: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        logger.info(
            "OPENAPI_LOG_V4|$userId|get init logs|$projectId|$pipelineId|$buildId|$debug|$elementId|$jobId" +
                "|$executeCount"
        )
        return client.get(ServiceLogResource::class).getInitLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            tag = elementId,
            jobId = jobId,
            executeCount = executeCount,
            debug = debug
        )
    }

    override fun getMoreLogs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        debug: Boolean?,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        logger.info(
            "OPENAPI_LOG_V4|$userId|get more logs|$projectId|$pipelineId|$buildId|$debug|$num|$fromStart" +
                "|$start|$end|$tag|$jobId|$executeCount"
        )
        return client.get(ServiceLogResource::class).getMoreLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            debug = debug,
            num = num ?: 100,
            fromStart = fromStart,
            start = start,
            end = end,
            tag = tag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun getAfterLogs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        start: Long,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        logger.info(
            "OPENAPI_LOG_V4|$userId|get after logs|$projectId|$pipelineId|$buildId|$start|$debug|$tag" +
                "|$jobId|$executeCount"
        )
        return client.get(ServiceLogResource::class).getAfterLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            start = start,
            debug = debug,
            tag = tag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun downloadLogs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Response {
        logger.info("OPENAPI_LOG_V4|$userId|download logs|$projectId|$pipelineId|$buildId|$tag|$jobId|$executeCount")
        checkPipelineId(projectId, pipelineId, buildId)
        val path = StringBuilder("$gatewayUrl/log/api/service/logs/")
        path.append(projectId)
        path.append("/$pipelineId/$buildId/download?executeCount=${executeCount ?: 1}")

        if (!tag.isNullOrBlank()) path.append("&tag=$tag")
        if (!jobId.isNullOrBlank()) path.append("&jobId=$jobId")
        val headers = mutableMapOf(AUTH_HEADER_USER_ID to userId, AUTH_HEADER_PROJECT_ID to projectId)
        val devopsToken = EnvironmentUtil.gatewayDevopsToken()
        if (devopsToken != null) {
            headers["X-DEVOPS-TOKEN"] = devopsToken
        }
        val response = OkhttpUtils.doLongGet(
            url = path.toString(),
            headers = headers
        )
        return Response
            .ok(response.body!!.byteStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $pipelineId-$buildId-log.txt")
            .header("Cache-Control", "no-cache")
            .build()
    }

    override fun getLogMode(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        tag: String,
        executeCount: Int?
    ): Result<QueryLogStatus> {
        logger.info("OPENAPI_LOG_V4|$userId|get log mode|$projectId|$pipelineId|$buildId|$tag|$executeCount")
        return client.get(ServiceLogResource::class).getLogMode(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            tag = tag,
            executeCount = executeCount
        )
    }

    override fun getLogLastLineNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<QueryLogLineNum> {
        logger.info("OPENAPI_LOG_V4|$userId|get log last line num|$projectId|$pipelineId|$buildId")
        return client.get(ServiceLogResource::class).getLogLastLineNum(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    private fun checkPipelineId(projectId: String, pipelineId: String?, buildId: String): String {
        val pipelineIdFormDB = indexService.getHandle(buildId) {
            kotlin.runCatching {
                client.get(ServiceBuildResource::class).getPipelineIdFromBuildId(projectId, buildId).data
            }.getOrElse {
                throw ParamBlankException(
                    it.message ?: "Invalid buildId, please check if projectId & buildId are related"
                )
            } ?: throw ParamBlankException("Invalid buildId")
        }
        if (pipelineId != null && pipelineId != pipelineIdFormDB) {
            throw ParamBlankException("PipelineId is invalid ")
        }
        return pipelineIdFormDB
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwLogResourceV4Impl::class.java)
    }
}
