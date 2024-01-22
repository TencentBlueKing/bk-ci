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

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.job.ServiceJobResource
import com.tencent.devops.environment.pojo.job.req.CreateAccountReq
import com.tencent.devops.environment.pojo.job.req.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.req.FileDistributeReq
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import com.tencent.devops.environment.pojo.job.req.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.req.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.req.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.resp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.resp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.resp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.resp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.OpOperateResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.resp.TaskTerminateResult
import com.tencent.devops.environment.service.job.JobService
import com.tencent.devops.environment.service.job.NodeScheduledService
import com.tencent.devops.environment.service.job.OpService
import com.tencent.devops.environment.service.job.PermissionManageService
import com.tencent.devops.environment.service.job.StockDataUpdateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceJobResourceImpl @Autowired constructor(
    private val jobService: JobService,
    private val opService: OpService,
    private val permissionManageService: PermissionManageService,
    private val stockDataUpdateService: StockDataUpdateService
) : ServiceJobResource {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceJobResourceImpl::class.java)
        private const val EXPIRATION_TIME_THREE_MONTH = "three months"
        private const val EXPIRATION_TIME_ONE_MONTH = "one month"
    }

    override fun executeScript(
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult> {
        checkParamBlank(userId, projectId)
        val jobResult = jobService.executeScript(projectId, userId, scriptExecuteReq)
        jobResult.data?.let { recordJobInsToProj(projectId, it.jobInstanceId, userId) }
        return jobResult
    }

    override fun distributeFile(
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult> {
        checkParamBlank(userId, projectId)
        val jobResult = jobService.distributeFile(projectId, userId, fileDistributeReq)
        jobResult.data?.let { recordJobInsToProj(projectId, it.jobInstanceId, userId) }
        return jobResult
    }

    override fun terminateTask(
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): JobResult<TaskTerminateResult> {
        checkParamBlank(userId, projectId)
        checkJobInsBelongToProj(projectId, taskTerminateReq.jobInstanceId)
        return jobService.terminateTask(taskTerminateReq)
    }

    override fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        checkParamBlank(userId, projectId)
        checkJobInsBelongToProj(projectId, jobInstanceId)
        return jobService.queryJobInstanceStatus(projectId, jobInstanceId, returnIpResult)
    }

    override fun queryJobInstanceLogs(
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        checkParamBlank(userId, projectId)
        checkJobInsBelongToProj(projectId, queryJobInstanceLogsReq.jobInstanceId)
        return jobService.queryJobInstanceLogs(queryJobInstanceLogsReq)
    }

    override fun createAccount(
        userId: String,
        projectId: String,
        createAccountReq: CreateAccountReq
    ): JobResult<CreateAccountResult> {
        checkParamBlank(userId, projectId)
        logger.info("[createAccount] userId:$userId, projectId:$projectId")
        return jobService.createAccount(createAccountReq)
    }

    override fun deleteAccount(
        userId: String,
        projectId: String,
        deleteAccountReq: DeleteAccountReq
    ): JobResult<DeleteAccountResult> {
        checkParamBlank(userId, projectId)
        logger.info("[deleteAccount] userId:$userId, projectId:$projectId")
        return jobService.deleteAccount(deleteAccountReq)
    }

    override fun getAccountList(
        userId: String,
        projectId: String,
        account: String?,
        alias: String?,
        category: Int?,
        start: Int?,
        length: Int?
    ): JobResult<GetAccountListResult> {
        checkParamBlank(userId, projectId)
        logger.info("[getAccountList] userId:$userId, projectId:$projectId")
        return jobService.getAccountList(projectId, account, alias, category, start, length)
    }

    override fun getStepInstanceDetail(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult> {
        checkParamBlank(userId, projectId)
        return jobService.getStepInstanceDetail(projectId, jobInstanceId, stepInstanceId)
    }

    override fun getStepInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        stepInstanceId: Long,
        executeCount: Int?,
        batch: Int?,
        maxHostNumPerGroup: Int?,
        keyword: String?,
        searchIp: String?,
        status: Int?,
        tag: String?
    ): JobResult<GetStepInstanceStatusResult> {
        checkParamBlank(userId, projectId)
        return jobService.getStepInstanceStatus(
            projectId, jobInstanceId, stepInstanceId, executeCount,
            batch, maxHostNumPerGroup, keyword, searchIp, status, tag
        )
    }

    override fun operateOpProject(userId: String, opOperateReq: OpOperateReq): OpOperateResult {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        return opService.operateOpProject(userId, opOperateReq)
    }

    override fun writeDisplayName(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        stockDataUpdateService.writeDisplayNameOnce()
    }

    override fun agentUpdate(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        stockDataUpdateService.scheduledUpdateAgent()
    }

    override fun checkDeployNodesInCC(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        stockDataUpdateService.checkDeployNodes()
    }

    private fun checkParamBlank(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("userId is blank.")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId is blank.")
        }
    }

    private fun checkJobInsBelongToProj(projectId: String, jobInstanceId: Long) {
        if (!permissionManageService.isJobInsBelongToProj(projectId, jobInstanceId)) {
            val expirationTime =
                if (logger.isDebugEnabled) EXPIRATION_TIME_THREE_MONTH
                else EXPIRATION_TIME_ONE_MONTH
            throw OauthForbiddenException(
                message = "The job instance you have queried doesn't belong to the current project " +
                    "or more than " + expirationTime + "."
            )
        }
    }

    private fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        permissionManageService.recordJobInsToProj(projectId, jobInstanceId, createUser)
    }
}
