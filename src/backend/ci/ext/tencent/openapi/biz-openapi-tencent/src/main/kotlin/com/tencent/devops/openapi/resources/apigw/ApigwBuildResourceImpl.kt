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
package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwBuildResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceImpl @Autowired constructor(
    private val client: Client
) : ApigwBuildResource {
    override fun manualStartupInfo(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        logger.info("get the pipeline($pipelineId) of project($projectId) manual startup info  by user($userId)")
        return client.get(ServiceBuildResource::class).manualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
        )
    }

    override fun getHistoryBuild(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<BuildHistoryPage<BuildHistory>> {
        logger.info("get the pipeline($pipelineId) of project($projectId) build history  by user($userId)")
        return client.get(ServiceBuildResource::class).getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS
        )
    }

    override fun start(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>
    ): Result<BuildId> {
        logger.info("Start the pipeline($pipelineId) of project($projectId) with param($values) by user($userId)")
        return client.get(ServiceBuildResource::class).manualStartup(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            values = values,
            channelCode = ChannelCode.BS
        )
    }

    override fun stop(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<Boolean> {
        logger.info("Stop the build($buildId) of pipeline($pipelineId) of project($projectId) by user($userId)")
        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }

    override fun getStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<BuildHistoryWithVars> {
        logger.info("Get the build($buildId) status of project($projectId) and pipeline($pipelineId) by user($userId)")
        return client.get(ServiceBuildResource::class).getBuildStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceImpl::class.java)
    }
}
