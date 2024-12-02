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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.job.TencentServiceJobResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_JOB_INSTANCE_NOT_BELONG_TO_PROJECT
import com.tencent.devops.environment.pojo.job.agentreq.ApiGwInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.ObtainManualCommandResult
import com.tencent.devops.environment.pojo.job.agentres.OperateStepInstanceResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.jobreq.CreateAccountReq
import com.tencent.devops.environment.pojo.job.jobreq.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.jobreq.FileDistributeReq
import com.tencent.devops.environment.pojo.job.jobreq.OpOperateReq
import com.tencent.devops.environment.pojo.job.jobreq.OperateStepInstanceReq
import com.tencent.devops.environment.pojo.job.jobreq.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobreq.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.jobreq.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.jobresp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.jobresp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.jobresp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.jobresp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.JobResult
import com.tencent.devops.environment.pojo.job.jobresp.OpOperateResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.jobresp.TaskTerminateResult
import com.tencent.devops.environment.service.gseagent.InstallTaskService
import com.tencent.devops.environment.service.job.ApiGwAgentService
import com.tencent.devops.environment.service.job.JobService
import com.tencent.devops.environment.service.job.OpService
import com.tencent.devops.environment.service.job.PermissionManageService
import com.tencent.devops.environment.service.job.TencentStockDataUpdateService
import com.tencent.devops.environment.service.sync.UpdateCmdbNodeService
import com.tencent.devops.environment.service.sync.UpdateGseAgentInfoService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class TencentServiceJobResourceImpl @Autowired constructor(
    private val jobService: JobService,
    private val opService: OpService,
    private val permissionManageService: PermissionManageService,
    private val updateCmdbNodeService: UpdateCmdbNodeService,
    private val updateGseAgentInfoService: UpdateGseAgentInfoService,
    private val tencentStockDataUpdateService: TencentStockDataUpdateService,
    private val installTaskService: InstallTaskService,
    private val apiGwAgentService: ApiGwAgentService
) : TencentServiceJobResource {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentServiceJobResourceImpl::class.java)
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

    override fun operateStepInstance(
        userId: String,
        projectId: String,
        operateStepInstanceReq: OperateStepInstanceReq
    ): JobResult<OperateStepInstanceResult> {
        checkParamBlank(userId, projectId)
        return jobService.operateStepInstance(operateStepInstanceReq)
    }

    override fun operateOpProject(userId: String, opOperateReq: OpOperateReq): OpOperateResult {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        return opService.operateOpProject(userId, opOperateReq)
    }

    override fun checkDeployNodesInCmdb(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        updateCmdbNodeService.updateCmdbNodeInfo()
    }

    override fun updateGseAgent(userId: String) {
        updateGseAgentInfoService.updateGseAgentStatusAndVersion()
    }

    override fun addStockNodeToCC(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        tencentStockDataUpdateService.addNodesToCCOnce()
    }

    override fun writeServerId(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("userId is blank.")
        tencentStockDataUpdateService.writeServerIdOnce()
    }

    override fun installAgent(
        userId: String,
        projectId: String,
        apiGwInstallAgentReq: ApiGwInstallAgentReq
    ): AgentResult<InstallAgentResult> {
        checkParamBlank(userId, projectId)
        checkCloudIpIsValid(apiGwInstallAgentReq.bkCloudId, apiGwInstallAgentReq.innerIp)
        checkInstallPermission(projectId, apiGwInstallAgentReq.bkCloudId!!, apiGwInstallAgentReq.innerIp!!)
        return apiGwAgentService.installAgent(userId, projectId, apiGwInstallAgentReq)
    }

    override fun queryAgentTaskStatus(
        userId: String,
        projectId: String,
        jobId: Int,
        queryAgentTaskStatusReq: QueryAgentTaskStatusReq
    ): AgentResult<QueryAgentTaskStatusResult> {
        checkParamBlank(userId, projectId)
        return installTaskService.queryAgentInstallTaskStatus(jobId, queryAgentTaskStatusReq)
    }

    override fun obtainManualInstallationCommand(
        userId: String,
        projectId: String,
        jobId: Int,
        innerIp: String,
        bkCloudId: Int
    ): AgentResult<ObtainManualCommandResult> {
        checkParamBlank(userId, projectId)
        checkCloudIpIsValid(bkCloudId, innerIp)
        return apiGwAgentService.getInstallCommand(jobId, bkCloudId, innerIp)
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
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = I18nUtil.getCodeLanMessage(ERROR_JOB_INSTANCE_NOT_BELONG_TO_PROJECT)
            )
        }
    }

    private fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        permissionManageService.recordJobInsToProj(projectId, jobInstanceId, createUser)
    }

    private fun checkCloudIpIsValid(cloudAreaId: Int?, ip: String?) {
        if (null == cloudAreaId) {
            throw ParamBlankException("cloudAreaId is null.")
        }
        if (ip.isNullOrBlank()) {
            throw ParamBlankException("ip is blank.")
        }
        val pattern = Regex(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        if (!pattern.matches(ip)) {
            throw InvalidParamException("$ip is invalid ip address")
        }
    }

    /*
    * 判断待安装 GSE Agent 的主机是否属于当前项目
     */
    private fun checkInstallPermission(projectId: String, cloudAreaId: Int, ip: String) {
        permissionManageService.checkInstallPermission(projectId, cloudAreaId, ip)
    }
}
