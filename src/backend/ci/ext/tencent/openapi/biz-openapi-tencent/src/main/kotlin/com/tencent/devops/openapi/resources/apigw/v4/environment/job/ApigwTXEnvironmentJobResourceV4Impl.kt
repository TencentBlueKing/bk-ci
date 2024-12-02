package com.tencent.devops.openapi.resources.apigw.v4.environment.job

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.job.TencentServiceJobResource
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
import com.tencent.devops.openapi.api.apigw.v4.environment.job.ApigwTXEnvironmentJobResourceV4
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXEnvironmentJobResourceV4Impl @Autowired constructor(
    val client: Client,
) : ApigwTXEnvironmentJobResourceV4 {

    override fun executeScript(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult> {
        return client.get(TencentServiceJobResource::class).executeScript(userId, projectId, scriptExecuteReq)
    }

    override fun distributeFile(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult> {
        return client.get(TencentServiceJobResource::class).distributeFile(userId, projectId, fileDistributeReq)
    }

    override fun terminateTask(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): JobResult<TaskTerminateResult> {
        return client.get(TencentServiceJobResource::class).terminateTask(userId, projectId, taskTerminateReq)
    }

    override fun queryJobInstanceStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        return client.get(TencentServiceJobResource::class).queryJobInstanceStatus(
            userId, projectId, jobInstanceId, returnIpResult
        )
    }

    override fun queryJobInstanceLogs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        return client.get(
            TencentServiceJobResource::class
        ).queryJobInstanceLogs(
            userId, projectId, queryJobInstanceLogsReq
        )
    }

    override fun createAccount(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        createAccountReq: CreateAccountReq
    ): JobResult<CreateAccountResult> {
        return client.get(TencentServiceJobResource::class).createAccount(userId, projectId, createAccountReq)
    }

    override fun deleteAccount(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        deleteAccountReq: DeleteAccountReq
    ): JobResult<DeleteAccountResult> {
        return client.get(TencentServiceJobResource::class).deleteAccount(userId, projectId, deleteAccountReq)
    }

    override fun getAccountList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        account: String?,
        alias: String?,
        category: Int?,
        start: Int?,
        length: Int?
    ): JobResult<GetAccountListResult> {
        return client.get(TencentServiceJobResource::class).getAccountList(
            userId, projectId, account, alias, category, start, length
        )
    }

    override fun getStepInstanceDetail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult> {
        return client.get(TencentServiceJobResource::class).getStepInstanceDetail(
            userId, projectId, jobInstanceId, stepInstanceId
        )
    }

    override fun getStepInstanceStatus(
        appCode: String?,
        apigwType: String?,
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
        return client.get(TencentServiceJobResource::class).getStepInstanceStatus(
            userId, projectId, jobInstanceId, stepInstanceId, executeCount,
            batch, maxHostNumPerGroup, keyword, searchIp, status, tag
        )
    }

    override fun operateStepInstance(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        operateStepInstanceReq: OperateStepInstanceReq
    ): JobResult<OperateStepInstanceResult> {
        return client.get(TencentServiceJobResource::class).operateStepInstance(
            userId, projectId, operateStepInstanceReq
        )
    }

    override fun operateOpProject(userId: String, opOperateReq: OpOperateReq): OpOperateResult {
        return client.get(TencentServiceJobResource::class).operateOpProject(userId, opOperateReq)
    }

    override fun checkDeployNodesInCmdb(userId: String) {
        client.get(TencentServiceJobResource::class).checkDeployNodesInCmdb(userId)
    }

    override fun updateGseAgent(userId: String) {
        client.get(TencentServiceJobResource::class).updateGseAgent(userId)
    }

    override fun addStockNodeToCC(userId: String) {
        client.get(TencentServiceJobResource::class).addStockNodeToCC(userId)
    }

    override fun writeServerId(userId: String) {
        client.get(TencentServiceJobResource::class).writeServerId(userId)
    }

    override fun installAgent(
        userId: String,
        apigwType: String?,
        projectId: String,
        apiGwInstallAgentReq: ApiGwInstallAgentReq
    ): AgentResult<InstallAgentResult> {
        return client.get(TencentServiceJobResource::class).installAgent(userId, projectId, apiGwInstallAgentReq)
    }

    override fun queryAgentTaskStatus(
        userId: String,
        apigwType: String?,
        projectId: String,
        jobId: Int,
        page: Int,
        pageSize: Int
    ): AgentResult<QueryAgentTaskStatusResult> {
        val queryAgentTaskStatusReq = QueryAgentTaskStatusReq(
            page = page,
            pageSize = pageSize
        )
        return client.get(TencentServiceJobResource::class)
            .queryAgentTaskStatus(userId, projectId, jobId, queryAgentTaskStatusReq)
    }

    override fun obtainManualInstallationCommand(
        userId: String,
        apigwType: String?,
        projectId: String,
        jobId: Int,
        innerIp: String,
        bkCloudId: Int
    ): AgentResult<ObtainManualCommandResult> {
        return client.get(TencentServiceJobResource::class)
            .obtainManualInstallationCommand(userId, projectId, jobId, innerIp, bkCloudId)
    }
}
