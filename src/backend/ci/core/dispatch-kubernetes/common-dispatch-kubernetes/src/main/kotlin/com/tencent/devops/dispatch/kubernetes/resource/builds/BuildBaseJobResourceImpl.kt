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

package com.tencent.devops.dispatch.kubernetes.resource.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.api.builds.BuildBaseJobResource
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobLogResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.service.DispatchBaseJobService
import com.tencent.devops.dispatch.kubernetes.service.DispatchBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildBaseJobResourceImpl @Autowired constructor(
    private val dispatchBaseJobService: DispatchBaseJobService,
    private val dispatchBuildService: DispatchBuildService
) : BuildBaseJobResource {
    override fun createJob(
        userId: String,
        projectId: String,
        buildId: String,
        jobReq: DispatchJobReq
    ): Result<DispatchTaskResp> {
        return Result(
            dispatchBaseJobService.createJob(
                userId = userId,
                projectId = projectId,
                buildId = buildId,
                jobReq = jobReq
            )
        )
    }

    override fun getJobStatus(
        userId: String,
        projectId: String,
        buildId: String,
        jobName: String
    ): Result<DispatchBuildStatusResp> {
        return Result(
            dispatchBaseJobService.getJobStatus(
                userId = userId,
                projectId = projectId,
                buildId = buildId,
                jobName = jobName
            )
        )
    }

    override fun getJobLogs(
        userId: String,
        projectId: String,
        buildId: String,
        jobName: String,
        sinceTime: Int?
    ): Result<DispatchJobLogResp> {
        return Result(
            dispatchBaseJobService.getJobLogs(
                userId = userId,
                projectId = projectId,
                buildId = buildId,
                jobName = jobName,
                sinceTime = sinceTime
            )
        )
    }

    override fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        buildImageReq: DispatchBuildImageReq
    ): Result<DispatchTaskResp> {
        return Result(
            dispatchBuildService.buildAndPushImage(
                userId = userId,
                projectId = projectId,
                buildId = buildId,
                dispatchBuildImageReq = buildImageReq
            )
        )
    }
}
