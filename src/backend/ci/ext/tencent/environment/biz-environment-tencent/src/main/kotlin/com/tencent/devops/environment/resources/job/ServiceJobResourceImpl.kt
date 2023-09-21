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
import com.tencent.devops.environment.pojo.job.JobCloudAccount
import com.tencent.devops.environment.pojo.job.FileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.TaskTerminateResult
import com.tencent.devops.environment.pojo.job.JobCloudExecuteTarget
import com.tencent.devops.environment.pojo.job.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.JobCloudFileSource
import com.tencent.devops.environment.pojo.job.JobCloudHost
import com.tencent.devops.environment.pojo.job.JobCloudQueryJobInstanceLogsReq
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
        val jobCloudScriptExecuteReq = JobCloudScriptExecuteReq(
            bkScopeType = "",
            bkScopeId = "",
            scriptContent = scriptExecuteReq.scriptContent,
            scriptParam = scriptExecuteReq.scriptParam,
            timeout = scriptExecuteReq.timeout,
            accountAlias = scriptExecuteReq.account,
            isParamSensitive = scriptExecuteReq.isSensiveParam,
            scriptLanguage = scriptExecuteReq.scriptLanguage,
            targetServer = JobCloudExecuteTarget(
                dynamicGroupList = scriptExecuteReq.executeTarget.envHashIdList,
                topoNodeList = scriptExecuteReq.executeTarget.nodeHashIdList,
                ipList = scriptExecuteReq.executeTarget.hostList?.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId ?: 0,
                        bkCloudId = it.bkCloudId ?: 0,
                        ip = it.ip ?: ""
                    )
                }
            ),
            bkAppCode = "",
            bkAppSecret = "",
            bkUsername = userId
        )
        return scriptExecuteService.executeScript(jobCloudScriptExecuteReq)
    }

    override fun distributeFile(
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): Result<FileDistributeResult> {
        checkParam(userId, projectId)
        val jobCloudFileDistributeReq = JobCloudFileDistributeReq(
            bkScopeType = "",
            bkScopeId = "",
            fileSourceList = fileDistributeReq.fileSourceList.map { fileSourceList ->
                JobCloudFileSource(
                    fileList = fileSourceList.fileList.toList(),
                    server = JobCloudExecuteTarget(
                        dynamicGroupList = fileSourceList.sourceFileTarget.envHashIdList,
                        topoNodeList = fileSourceList.sourceFileTarget.nodeHashIdList,
                        ipList = fileSourceList.sourceFileTarget.hostList?.map {
                            JobCloudHost(
                                bkHostId = it.bkHostId ?: 0,
                                bkCloudId = it.bkCloudId ?: 0,
                                ip = it.ip ?: ""
                            )
                        }
                    ),
                    account = JobCloudAccount(
                        id = null,
                        alias = fileDistributeReq.account
                    )
                )
            },
            fileTargetPath = fileDistributeReq.fileTargetPath,
            executeTarget = JobCloudExecuteTarget(
                dynamicGroupList = fileDistributeReq.executeTarget.envHashIdList,
                topoNodeList = fileDistributeReq.executeTarget.nodeHashIdList,
                ipList = fileDistributeReq.executeTarget.hostList?.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId ?: 0,
                        bkCloudId = it.bkCloudId ?: 0,
                        ip = it.ip ?: ""
                    )
                }
            ),
            accountAlias = fileDistributeReq.account,
            timeout = fileDistributeReq.timeout,
            bkAppCode = "",
            bkAppSecret = "",
            bkUsername = userId
        )
        return fileDistributeService.distributeFile(jobCloudFileDistributeReq)
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
        jobInstanceId: Long,
        returnIpResult: Boolean
    ): Result<QueryJobInstanceStatusResult> {
        checkParam(userId, projectId)
        return queryJobInstanceStatusService.queryJobInstanceStatus(userId, projectId, jobInstanceId, returnIpResult)
    }

    override fun queryJobInstanceLogs(
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): Result<QueryJobInstanceLogsResult> {
        checkParam(userId, projectId)
        val jobCloudQueryJobInstanceLogsReq = JobCloudQueryJobInstanceLogsReq(
            bkScopeType = "",
            bkScopeId = "",
            jobInstanceId = queryJobInstanceLogsReq.jobInstanceId,
            stepInstanceId = queryJobInstanceLogsReq.stepInstanceId,
            hostList = queryJobInstanceLogsReq.hostIdList?.map {
                JobCloudHost(
                    bkHostId = it,
                    bkCloudId = null,
                    ip = null
                )
            },
            bkAppCode = "",
            bkAppSecret = "",
            bkUsername = userId
        )
        return queryJobInstanceLogsService.queryJobInstanceLogs(jobCloudQueryJobInstanceLogsReq)
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
