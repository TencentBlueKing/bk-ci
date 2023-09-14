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

package com.tencent.devops.environment.resources.job

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.job.ServiceJobResource
import com.tencent.devops.environment.pojo.job.FileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.TaskTerminateResult
import com.tencent.devops.environment.service.job.ScriptExecuteService
import com.tencent.devops.environment.service.job.FileDistributeService
import com.tencent.devops.environment.service.job.TaskTerminateService
import com.tencent.devops.environment.service.job.QueryJobInstanceStatusService
import com.tencent.devops.environment.service.job.QueryJobInstanceLogsService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceJobResourceImpl @Autowired constructor(
    private val scriptExecuteService: ScriptExecuteService,
    private val fileDistributeService: FileDistributeService,
    private val taskTerminateService: TaskTerminateService,
    private val queryJobInstanceStatusService: QueryJobInstanceStatusService,
    private val queryJobInstanceLogsService: QueryJobInstanceLogsService
) : ServiceJobResource {
    override fun executeScript(
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): Result<ScriptExecuteResult> {
        checkParam(userId, projectId)
        return Result(scriptExecuteService.executeScript(userId, projectId, scriptExecuteReq))
    }

    override fun distributeFile(
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): Result<FileDistributeResult> {
        checkParam(userId, projectId)
        return Result(fileDistributeService.distributeFile(userId, projectId, fileDistributeReq))
    }

    override fun terminateTask(
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): Result<TaskTerminateResult> {
        checkParam(userId, projectId)
        return Result(taskTerminateService.terminateTask(userId, projectId, taskTerminateReq))
    }

    override fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long
    ): Result<QueryJobInstanceStatusResult> {
        checkParam(userId, projectId)
        return Result(queryJobInstanceStatusService.queryJobInstanceStatus(userId, projectId, jobInstanceId))
    }

    override fun queryJobInstanceLogs(
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): Result<QueryJobInstanceLogsResult> {
        checkParam(userId, projectId)
        return Result(queryJobInstanceLogsService.queryJobInstanceLogs(userId, projectId, queryJobInstanceLogsReq))
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("userId is blank.")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId is blank.")
        }
    }
}
