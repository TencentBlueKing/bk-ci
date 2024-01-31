package com.tencent.devops.openapi.resources.apigw.v4.job

import com.tencent.devops.common.client.Client
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
import com.tencent.devops.openapi.api.apigw.v4.job.ApigwJobResourceV4
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwJobResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwJobResourceV4 {

    override fun executeScript(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult> {
        return client.get(ServiceJobResource::class).executeScript(userId, projectId, scriptExecuteReq)
    }

    override fun distributeFile(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult> {
        return client.get(ServiceJobResource::class).distributeFile(userId, projectId, fileDistributeReq)
    }

    override fun terminateTask(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): JobResult<TaskTerminateResult> {
        return client.get(ServiceJobResource::class).terminateTask(userId, projectId, taskTerminateReq)
    }

    override fun queryJobInstanceStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        return client.get(ServiceJobResource::class).queryJobInstanceStatus(
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
        return client.get(ServiceJobResource::class).queryJobInstanceLogs(userId, projectId, queryJobInstanceLogsReq)
    }

    override fun createAccount(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        createAccountReq: CreateAccountReq
    ): JobResult<CreateAccountResult> {
        return client.get(ServiceJobResource::class).createAccount(userId, projectId, createAccountReq)
    }

    override fun deleteAccount(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        deleteAccountReq: DeleteAccountReq
    ): JobResult<DeleteAccountResult> {
        return client.get(ServiceJobResource::class).deleteAccount(userId, projectId, deleteAccountReq)
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
        return client.get(ServiceJobResource::class).getAccountList(
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
        return client.get(ServiceJobResource::class).getStepInstanceDetail(
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
        return client.get(ServiceJobResource::class).getStepInstanceStatus(
            userId, projectId, jobInstanceId, stepInstanceId, executeCount,
            batch, maxHostNumPerGroup, keyword, searchIp, status, tag
        )
    }

    override fun operateOpProject(userId: String, opOperateReq: OpOperateReq): OpOperateResult {
        return client.get(ServiceJobResource::class).operateOpProject(userId, opOperateReq)
    }

    override fun writeDisplayName(userId: String) {
        client.get(ServiceJobResource::class).writeDisplayName(userId)
    }

    override fun updateDevopsAgent(userId: String) {
        client.get(ServiceJobResource::class).updateDevopsAgent(userId)
    }

    override fun checkDeployNodesInCC(userId: String) {
        client.get(ServiceJobResource::class).checkDeployNodesInCC(userId)
    }
}
