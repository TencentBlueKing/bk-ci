package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.CreateAccountReq
import com.tencent.devops.environment.pojo.job.CreateAccountResult
import com.tencent.devops.environment.pojo.job.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.FileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.GetAccountListResult
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.TaskTerminateResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAccount
import com.tencent.devops.environment.pojo.job.req.JobCloudCreateAccountReq
import com.tencent.devops.environment.pojo.job.req.JobCloudDeleteAccountReq
import com.tencent.devops.environment.pojo.job.req.JobCloudExecuteTarget
import com.tencent.devops.environment.pojo.job.req.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.req.JobCloudFileSource
import com.tencent.devops.environment.pojo.job.req.JobCloudHost
import com.tencent.devops.environment.pojo.job.req.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.req.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.req.JobCloudTaskTerminateReq
import com.tencent.devops.environment.service.job.api.ApigwJobCloudApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("JobService")
class JobService @Autowired constructor(
    private val apigwJobCloudApi: ApigwJobCloudApi
) {
    fun executeScript(userId: String, scriptExecuteReq: ScriptExecuteReq): JobResult<ScriptExecuteResult> {
        val dynamicGroupList: List<JobCloudHost> = emptyList()
        val topoNodeList: List<JobCloudHost> = emptyList()
        val jobCloudScriptExecuteReq = JobCloudScriptExecuteReq(
            scriptContent = scriptExecuteReq.scriptContent,
            scriptParam = scriptExecuteReq.scriptParam,
            timeout = scriptExecuteReq.timeout,
            accountAlias = scriptExecuteReq.account,
            isParamSensitive = scriptExecuteReq.isSensiveParam,
            scriptLanguage = scriptExecuteReq.scriptLanguage,
            targetServer = JobCloudExecuteTarget(
                hostList = scriptExecuteReq.executeTarget.hostList.filter { it.bkHostId == null }.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId,
                        bkCloudId = it.bkCloudId,
                        ip = it.ip
                    )
                },
                hostIdList = scriptExecuteReq.executeTarget.hostList.filter { it.bkHostId != null }.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId,
                        bkCloudId = it.bkCloudId,
                        ip = it.ip
                    )
                }
            ),
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::executeScript.name)
        return apigwJobCloudApi.executePostRequest(userId, jobCloudScriptExecuteReq, ScriptExecuteResult::class.java)
    }

    fun distributeFile(userId: String, fileDistributeReq: FileDistributeReq): JobResult<FileDistributeResult> {
        val jobCloudFileDistributeReq = JobCloudFileDistributeReq(
            fileSourceList = fileDistributeReq.fileSourceList.map { fileSource ->
                JobCloudFileSource(
                    fileList = fileSource.fileList.toList(),
                    server = JobCloudExecuteTarget(
                        hostList = fileSource.sourceFileServer.hostList.map {
                            JobCloudHost(
                                bkHostId = it.bkHostId,
                                bkCloudId = it.bkCloudId,
                                ip = it.ip
                            )
                        },
                        hostIdList = fileDistributeReq.executeTarget.hostList.filter { it.bkHostId != null }.map {
                            JobCloudHost(
                                bkHostId = it.bkHostId,
                                bkCloudId = it.bkCloudId,
                                ip = it.ip
                            )
                        }
                    ),
                    account = JobCloudAccount(
                        id = fileSource.account.id,
                        alias = fileSource.account.alias
                    )
                )
            },
            fileTargetPath = fileDistributeReq.fileTargetPath,
            transferMode = fileDistributeReq.transferMode,
            executeTarget = JobCloudExecuteTarget(
                hostList = fileDistributeReq.executeTarget.hostList.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId,
                        bkCloudId = it.bkCloudId,
                        ip = it.ip
                    )
                },
                hostIdList = fileDistributeReq.executeTarget.hostList.filter { it.bkHostId != null }.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId,
                        bkCloudId = it.bkCloudId,
                        ip = it.ip
                    )
                }
            ),
            accountAlias = fileDistributeReq.accountAlias,
            accountId = fileDistributeReq.accountId,
            timeout = fileDistributeReq.timeout,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::distributeFile.name)
        return apigwJobCloudApi.executePostRequest(userId, jobCloudFileDistributeReq, FileDistributeResult::class.java)
    }

    fun terminateTask(userId: String, taskTerminateReq: TaskTerminateReq): JobResult<TaskTerminateResult> {
        val jobCloudTaskTerminateReq = JobCloudTaskTerminateReq(
            jobInstanceId = taskTerminateReq.jobInstanceId,
            operationCode = taskTerminateReq.operationCode,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::terminateTask.name)
        return apigwJobCloudApi.executePostRequest(userId, jobCloudTaskTerminateReq, TaskTerminateResult::class.java)
    }

    fun queryJobInstanceLogs(
        userId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        val jobCloudQueryJobInstanceLogsReq = JobCloudQueryJobInstanceLogsReq(
            jobInstanceId = queryJobInstanceLogsReq.jobInstanceId,
            stepInstanceId = queryJobInstanceLogsReq.stepInstanceId,
            hostList = queryJobInstanceLogsReq.hostList?.map {
                JobCloudHost(
                    bkHostId = it.bkHostId,
                    bkCloudId = it.bkCloudId,
                    ip = it.ip
                )
            },
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::queryJobInstanceLogs.name)
        return apigwJobCloudApi.executePostRequest(
            userId, jobCloudQueryJobInstanceLogsReq, QueryJobInstanceLogsResult::class.java
        )
    }

    fun createAccount(userId: String, createAccountReq: CreateAccountReq): JobResult<CreateAccountResult> {
        val jobCloudCreateAccountReq = JobCloudCreateAccountReq(
            account = createAccountReq.account,
            type = createAccountReq.type,
            category = createAccountReq.category,
            password = createAccountReq.password,
            alias = createAccountReq.alias,
            description = createAccountReq.description,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::createAccount.name)
        return apigwJobCloudApi.executePostRequest(userId, jobCloudCreateAccountReq, CreateAccountResult::class.java)
    }

    fun deleteAccount(userId: String, deleteAccountReq: DeleteAccountReq): JobResult<DeleteAccountResult> {
        val jobCloudDeleteAccountReq = JobCloudDeleteAccountReq(
            id = deleteAccountReq.id,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::deleteAccount.name)
        return apigwJobCloudApi.executePostRequest(userId, jobCloudDeleteAccountReq, DeleteAccountResult::class.java)
    }

    fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        ApigwJobCloudApi.setThreadLocal("queryJobInstanceStatus")
        return apigwJobCloudApi.executeGetRequest(
            userId, QueryJobInstanceStatusResult::class.java, jobInstanceId, returnIpResult ?: ""
        )
    }

    fun getAccountList(
        userId: String,
        projectId: String,
        account: String?,
        alias: String?,
        category: Int?,
        start: Int?,
        length: Int?
    ): JobResult<GetAccountListResult> {
        ApigwJobCloudApi.setThreadLocal("getAccountList")
        return apigwJobCloudApi.executeGetRequest(
            userId, GetAccountListResult::class.java,
            category ?: "", account ?: "", alias ?: "", start ?: "", length ?: ""
        )
    }
}