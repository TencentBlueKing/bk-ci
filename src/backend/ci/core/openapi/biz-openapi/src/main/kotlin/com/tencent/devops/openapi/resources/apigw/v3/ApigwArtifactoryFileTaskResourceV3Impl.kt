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

package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryFileTaskResource
import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwArtifactoryFileTaskResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryFileTaskResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactoryFileTaskResourceV3 {
    override fun createFileTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        createFileTaskReq: CreateFileTaskReq
    ): Result<String> {
        logger.info("OPENAPI_ARTIFACTORY_FILE_TASK_V3|$userId|create file task|$projectId|$pipelineId|$buildId")
        return client.get(ServiceArtifactoryFileTaskResource::class).createFileTask(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            createFileTaskReq = createFileTaskReq
        )
    }

    override fun getStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<FileTaskInfo?> {
        logger.info("OPENAPI_ARTIFACTORY_FILE_TASK_V3|$userId|get status|$projectId|$pipelineId|$buildId|$taskId")
        return client.get(ServiceArtifactoryFileTaskResource::class).getStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )
    }

    override fun clearFileTask(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_ARTIFACTORY_FILE_TASK_V3|$userId|clear file task|$projectId|$pipelineId|$buildId|$taskId")
        return client.get(ServiceArtifactoryFileTaskResource::class).clearFileTask(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactoryFileTaskResourceV3Impl::class.java)
    }
}
