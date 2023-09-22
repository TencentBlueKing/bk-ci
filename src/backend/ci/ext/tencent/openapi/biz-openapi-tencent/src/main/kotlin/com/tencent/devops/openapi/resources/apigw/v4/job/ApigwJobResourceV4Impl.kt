package com.tencent.devops.openapi.resources.apigw.v4.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
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
    ): Result<ScriptExecuteResult> {
        return client.get(ServiceJobResource::class).executeScript(userId, projectId, scriptExecuteReq)
    }

    override fun distributeFile(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): Result<FileDistributeResult> {
        return client.get(ServiceJobResource::class).distributeFile(userId, projectId, fileDistributeReq)
    }

    override fun terminateTask(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): Result<TaskTerminateResult> {
        return client.get(ServiceJobResource::class).terminateTask(userId, projectId, taskTerminateReq)
    }

    override fun queryJobInstanceStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): Result<QueryJobInstanceStatusResult> {
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
    ): Result<QueryJobInstanceLogsResult> {
        return client.get(ServiceJobResource::class).queryJobInstanceLogs(userId, projectId, queryJobInstanceLogsReq)
    }
}
