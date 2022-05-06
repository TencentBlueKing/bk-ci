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

package com.tencent.devops.dispatch.bcs.resources.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.bcs.actions.JobAction
import com.tencent.devops.dispatch.bcs.actions.TaskAction
import com.tencent.devops.dispatch.bcs.api.builds.BuildBcsResource
import com.tencent.devops.dispatch.bcs.pojo.DispatchBuildStatusResp
import com.tencent.devops.dispatch.bcs.pojo.DispatchJobLogResp
import com.tencent.devops.dispatch.bcs.pojo.DispatchJobReq
import com.tencent.devops.dispatch.bcs.pojo.DispatchTaskResp
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildBcsResourceImpl @Autowired constructor(
    private val jobAction: JobAction,
    private val taskAction: TaskAction
) : BuildBcsResource {
    override fun createJob(
        userId: String,
        projectId: String,
        buildId: String,
        jobReq: DispatchJobReq
    ): Result<DispatchTaskResp> {
        return Result(jobAction.createJob(userId, projectId, buildId, jobReq))
    }

    override fun getJobStatus(userId: String, jobName: String): Result<DispatchBuildStatusResp> {
        return Result(jobAction.getJobStatus(userId, jobName))
    }

    override fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): Result<DispatchJobLogResp> {
        return Result(jobAction.getJobLogs(userId, jobName, sinceTime))
    }

    override fun getTaskStatus(userId: String, taskId: String): Result<DispatchBuildStatusResp> {
        return Result(taskAction.getTaskStatus(userId, taskId))
    }
}
