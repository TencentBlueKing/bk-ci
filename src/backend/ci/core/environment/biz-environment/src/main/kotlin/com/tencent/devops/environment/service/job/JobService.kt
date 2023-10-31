package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.req.CreateAccountReq
import com.tencent.devops.environment.pojo.job.req.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudDeleteAccountResult
import com.tencent.devops.environment.pojo.job.req.FileDistributeReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudFileDistributeResult
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudGetAccountListResult
import com.tencent.devops.environment.pojo.job.req.Host
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudResult
import com.tencent.devops.environment.pojo.job.req.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudQueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudQueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.req.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudScriptExecuteResult
import com.tencent.devops.environment.pojo.job.req.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudTaskTerminateResult
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudAccount
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudCreateAccountReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudDeleteAccountReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudExecuteTarget
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudFileSource
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudHost
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudTaskTerminateReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudCreateAccountResult
import com.tencent.devops.environment.pojo.job.resp.AuthorizedAccount
import com.tencent.devops.environment.pojo.job.resp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.resp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.resp.FileDistributeLog
import com.tencent.devops.environment.pojo.job.resp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.resp.FileLog
import com.tencent.devops.environment.pojo.job.resp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.resp.HostInRes
import com.tencent.devops.environment.pojo.job.resp.JobInstance
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.JobStepInstance
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.ScriptExcuteLog
import com.tencent.devops.environment.pojo.job.resp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.resp.StepHostResult
import com.tencent.devops.environment.pojo.job.resp.TaskTerminateResult
import com.tencent.devops.environment.service.job.api.ApigwJobCloudApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("JobService")
class JobService @Autowired constructor(
    private val parseHashListService: ParseHashListService,
    private val apigwJobCloudApi: ApigwJobCloudApi
) {
    fun executeScript(
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult> {
        val allHostList: List<Host> = parseHashListService.getAllHostList(projectId, scriptExecuteReq.executeTarget)
        val jobCloudScriptExecuteReq = JobCloudScriptExecuteReq(
            scriptContent = scriptExecuteReq.scriptContent,
            scriptParam = scriptExecuteReq.scriptParam,
            timeout = scriptExecuteReq.timeout,
            accountAlias = scriptExecuteReq.accountAlias,
            accountId = scriptExecuteReq.accountId,
            isParamSensitive = scriptExecuteReq.isSensiveParam,
            scriptLanguage = scriptExecuteReq.scriptLanguage,
            targetServer = JobCloudExecuteTarget(
                hostList = allHostList.filter { it.bkHostId == null }.map {
                    JobCloudHost(
                        bkCloudId = it.bkCloudId ?: 0,
                        ip = it.ip
                    )
                },
                hostIdList = allHostList.filter { it.bkHostId != null }.map {
                    it.bkHostId ?: 0L
                }
            ),
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::executeScript.name)

        val jobCloudScriptExecuteRes: JobCloudResult<JobCloudScriptExecuteResult> = apigwJobCloudApi.executePostRequest(
            userId, jobCloudScriptExecuteReq, JobCloudScriptExecuteResult::class.java
        )
        val scriptExecuteRes: JobResult<ScriptExecuteResult> = JobResult(
            code = jobCloudScriptExecuteRes.code,
            result = jobCloudScriptExecuteRes.result,
            jobRequestId = jobCloudScriptExecuteRes.jobRequestId,
            data = jobCloudScriptExecuteRes.data?.let {
                ScriptExecuteResult(
                    jobInstanceId = it.jobInstanceId,
                    jobInstanceName = it.jobInstanceName,
                    stepInstanceId = it.stepInstanceId
                )
            }
        )
        return scriptExecuteRes
    }

    fun distributeFile(
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult> {
        val allExecuteTargetHostList: List<Host> = parseHashListService.getAllHostList(
            projectId, fileDistributeReq.executeTarget
        )
        val jobCloudFileDistributeReq = JobCloudFileDistributeReq(
            fileSourceList = fileDistributeReq.fileSourceList.map { fileSource ->
                val allFileSourceHostList: List<Host> = parseHashListService.getAllHostList(
                    projectId, fileSource.sourceFileServer
                )
                JobCloudFileSource(
                    fileList = fileSource.fileList.toList(),
                    server = JobCloudExecuteTarget(
                        hostList = allFileSourceHostList.filter { it.bkHostId == null }.map {
                            JobCloudHost(bkCloudId = it.bkCloudId ?: 0, ip = it.ip)
                        },
                        hostIdList = allFileSourceHostList.filter { it.bkHostId != null }.map { it.bkHostId ?: 0L }
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
                hostList = allExecuteTargetHostList.filter { it.bkHostId == null }.map {
                    JobCloudHost(bkCloudId = it.bkCloudId ?: 0, ip = it.ip)
                },
                hostIdList = allExecuteTargetHostList.filter { it.bkHostId != null }.map { it.bkHostId ?: 0L }
            ),
            accountAlias = fileDistributeReq.accountAlias,
            accountId = fileDistributeReq.accountId,
            timeout = fileDistributeReq.timeout,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::distributeFile.name)

        val jobCloudFileDistributeRes: JobCloudResult<JobCloudFileDistributeResult> =
            apigwJobCloudApi.executePostRequest(
                userId, jobCloudFileDistributeReq, JobCloudFileDistributeResult::class.java
            )
        val fileDistributeRes: JobResult<FileDistributeResult> = JobResult(
            code = jobCloudFileDistributeRes.code,
            result = jobCloudFileDistributeRes.result,
            jobRequestId = jobCloudFileDistributeRes.jobRequestId,
            data = jobCloudFileDistributeRes.data?.let {
                FileDistributeResult(
                    jobInstanceId = it.jobInstanceId,
                    jobInstanceName = it.jobInstanceName,
                    stepInstanceId = it.stepInstanceId
                )
            }
        )
        return fileDistributeRes
    }

    fun terminateTask(userId: String, taskTerminateReq: TaskTerminateReq): JobResult<TaskTerminateResult> {
        val jobCloudTaskTerminateReq = JobCloudTaskTerminateReq(
            jobInstanceId = taskTerminateReq.jobInstanceId,
            operationCode = taskTerminateReq.operationCode,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::terminateTask.name)

        val jobCloudTaskTerminateRes: JobCloudResult<JobCloudTaskTerminateResult> = apigwJobCloudApi.executePostRequest(
            userId, jobCloudTaskTerminateReq, JobCloudTaskTerminateResult::class.java
        )
        val taskTerminateRes: JobResult<TaskTerminateResult> = JobResult(
            code = jobCloudTaskTerminateRes.code,
            result = jobCloudTaskTerminateRes.result,
            jobRequestId = jobCloudTaskTerminateRes.jobRequestId,
            data = jobCloudTaskTerminateRes.data?.let {
                TaskTerminateResult(
                    jobInstanceId = it.jobInstanceId,
                    jobInstanceName = it.jobInstanceName,
                    stepInstanceId = it.stepInstanceId
                )
            }
        )
        return taskTerminateRes
    }

    fun queryJobInstanceLogs(
        userId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        val jobCloudQueryJobInstanceLogsReq = JobCloudQueryJobInstanceLogsReq(
            jobInstanceId = queryJobInstanceLogsReq.jobInstanceId,
            stepInstanceId = queryJobInstanceLogsReq.stepInstanceId,
            hostList = queryJobInstanceLogsReq.hostList?.map {
                JobCloudHost(bkHostId = it.bkHostId, bkCloudId = it.bkCloudId ?: 0, ip = it.ip)
            },
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::queryJobInstanceLogs.name)
        val jobCloudQueryJobInstanceLogsRes: JobCloudResult<JobCloudQueryJobInstanceLogsResult> =
            apigwJobCloudApi.executePostRequest(
                userId, jobCloudQueryJobInstanceLogsReq, JobCloudQueryJobInstanceLogsResult::class.java
            )
        val queryJobInstanceLogsRes: JobResult<QueryJobInstanceLogsResult> = JobResult(
            code = jobCloudQueryJobInstanceLogsRes.code,
            result = jobCloudQueryJobInstanceLogsRes.result,
            jobRequestId = jobCloudQueryJobInstanceLogsRes.jobRequestId,
            data = jobCloudQueryJobInstanceLogsRes.data?.let {
                QueryJobInstanceLogsResult(
                    jobInstanceId = it.jobInstanceId, stepInstanceId = it.stepInstanceId, logType = it.logType,
                    scriptTaskLogs = it.scriptTaskLogs?.map { scriptTaskLog ->
                        ScriptExcuteLog(
                            bkCloudId = scriptTaskLog.bkCloudId, ip = scriptTaskLog.ip,
                            bkHostId = scriptTaskLog.bkHostId, ipv6 = scriptTaskLog.ipv6,
                            logContent = scriptTaskLog.logContent
                        )
                    },
                    fileTaskLogs = it.fileTaskLogs?.map { fileTaskLog ->
                        FileDistributeLog(
                            bkCloudId = fileTaskLog.bkCloudId, ip = fileTaskLog.ip, bkHostId = fileTaskLog.bkHostId,
                            fileLogList = fileTaskLog.jobCloudFileLogList.map { jobCloudFileLog ->
                                FileLog(
                                    mode = jobCloudFileLog.mode,
                                    srcHost = jobCloudFileLog.srcHost.let { srcHost ->
                                        HostInRes(
                                            bkCloudId = srcHost.bkCloudId, ip = srcHost.ip,
                                            bkHostId = srcHost.bkHostId
                                        )
                                    },
                                    srcPath = jobCloudFileLog.srcPath,
                                    destHost = jobCloudFileLog.destHost.let { destHost ->
                                        HostInRes(
                                            bkCloudId = destHost?.bkCloudId, ip = destHost?.ip,
                                            bkHostId = destHost?.bkHostId
                                        )
                                    },
                                    destPath = jobCloudFileLog.destPath,
                                    status = jobCloudFileLog.status,
                                    logContent = jobCloudFileLog.logContent
                                )
                            }
                        )
                    }
                )
            }
        )
        return queryJobInstanceLogsRes
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

        val jobCloudCreateAccountRes: JobCloudResult<JobCloudCreateAccountResult> = apigwJobCloudApi.executePostRequest(
            userId, jobCloudCreateAccountReq, JobCloudCreateAccountResult::class.java
        )
        val createAccountRes: JobResult<CreateAccountResult> = JobResult(
            code = jobCloudCreateAccountRes.code,
            result = jobCloudCreateAccountRes.result,
            jobRequestId = jobCloudCreateAccountRes.jobRequestId,
            data = jobCloudCreateAccountRes.data?.let {
                CreateAccountResult(
                    id = it.id,
                    account = it.account,
                    type = it.type,
                    category = it.category,
                    alias = it.alias,
                    os = it.os,
                    description = it.description,
                    creator = it.creator,
                    createTime = it.createTime,
                    lastModifyUser = it.lastModifyUser,
                    lastModifyTime = it.lastModifyTime,
                    dbSystemAccountId = it.dbSystemAccountId,
                    bkBizId = it.bkBizId,
                    bkScopeType = it.bkScopeType,
                    bkScopeId = it.bkScopeId
                )
            }
        )
        return createAccountRes
    }

    fun deleteAccount(userId: String, deleteAccountReq: DeleteAccountReq): JobResult<DeleteAccountResult> {
        val jobCloudDeleteAccountReq = JobCloudDeleteAccountReq(
            id = deleteAccountReq.id,
            bkUsername = userId
        )
        ApigwJobCloudApi.setThreadLocal(::deleteAccount.name)

        val jobCloudDeleteAccountRes: JobCloudResult<JobCloudDeleteAccountResult> = apigwJobCloudApi.executePostRequest(
            userId, jobCloudDeleteAccountReq, JobCloudDeleteAccountResult::class.java
        )
        val deleteAccountRes: JobResult<DeleteAccountResult> = JobResult(
            code = jobCloudDeleteAccountRes.code,
            result = jobCloudDeleteAccountRes.result,
            jobRequestId = jobCloudDeleteAccountRes.jobRequestId,
            data = jobCloudDeleteAccountRes.data?.let {
                DeleteAccountResult(
                    id = it.id,
                    account = it.account,
                    type = it.type,
                    category = it.category,
                    alias = it.alias,
                    os = it.os,
                    description = it.description,
                    creator = it.creator,
                    createTime = it.createTime,
                    lastModifyUser = it.lastModifyUser,
                    lastModifyTime = it.lastModifyTime,
                    dbSystemAccountId = it.dbSystemAccountId,
                    bkBizId = it.bkBizId,
                    bkScopeType = it.bkScopeType,
                    bkScopeId = it.bkScopeId
                )
            }
        )
        return deleteAccountRes
    }

    fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        ApigwJobCloudApi.setThreadLocal("queryJobInstanceStatus")
        val jobCloudQueryJobInstanceStatusRes: JobCloudResult<JobCloudQueryJobInstanceStatusResult> =
            apigwJobCloudApi.executeGetRequest(
                userId, JobCloudQueryJobInstanceStatusResult::class.java, jobInstanceId, returnIpResult ?: ""
            )
        val queryJobInstanceStatusRes: JobResult<QueryJobInstanceStatusResult> = JobResult(
            code = jobCloudQueryJobInstanceStatusRes.code,
            result = jobCloudQueryJobInstanceStatusRes.result,
            jobRequestId = jobCloudQueryJobInstanceStatusRes.jobRequestId,
            data = jobCloudQueryJobInstanceStatusRes.data?.let {
                QueryJobInstanceStatusResult(
                    finished = it.finished,
                    jobInstance = it.jobCloudJobInstance?.let { jobInstance ->
                        JobInstance(
                            name = jobInstance.name,
                            status = jobInstance.status,
                            createTime = jobInstance.createTime,
                            startTime = jobInstance.startTime,
                            endTime = jobInstance.endTime,
                            totalTime = jobInstance.totalTime,
                            jobInstanceId = jobInstance.jobInstanceId,
                            bkBizId = jobInstance.bkBizId,
                            bkScopeType = jobInstance.bkScopeType,
                            bkScopeId = jobInstance.bkScopeId
                        )
                    },
                    stepInstanceList = it.stepInstanceList?.map { stepInstance ->
                        JobStepInstance(
                            stepInstanceId = stepInstance.stepInstanceId,
                            type = stepInstance.type,
                            name = stepInstance.name,
                            stepStatus = stepInstance.stepStatus,
                            createTime = stepInstance.createTime,
                            startTime = stepInstance.startTime,
                            endTime = stepInstance.endTime,
                            totalTime = stepInstance.totalTime,
                            stepRetries = stepInstance.stepRetries,
                            stepHostResultList = stepInstance.jobCloudStepHostResultList?.map { jobCldStepHostResult ->
                                StepHostResult(
                                    ip = jobCldStepHostResult.ip,
                                    bkHostId = jobCldStepHostResult.bkHostId,
                                    bkCloudId = jobCldStepHostResult.bkCloudId,
                                    status = jobCldStepHostResult.status,
                                    tag = jobCldStepHostResult.tag,
                                    exitCode = jobCldStepHostResult.exitCode,
                                    errorCode = jobCldStepHostResult.errorCode,
                                    startTime = jobCldStepHostResult.startTime,
                                    endTime = jobCldStepHostResult.endTime,
                                    totalTime = jobCldStepHostResult.totalTime
                                )
                            }
                        )
                    }
                )
            }
        )
        return queryJobInstanceStatusRes
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
        val jobCloudGetAccountListRes: JobCloudResult<JobCloudGetAccountListResult> =
            apigwJobCloudApi.executeGetRequest(
                userId, JobCloudGetAccountListResult::class.java,
                category ?: "", account ?: "", alias ?: "", start ?: "", length ?: ""
            )
        val getAccountListRes: JobResult<GetAccountListResult> = JobResult(
            code = jobCloudGetAccountListRes.code,
            result = jobCloudGetAccountListRes.result,
            jobRequestId = jobCloudGetAccountListRes.jobRequestId,
            data = jobCloudGetAccountListRes.data?.let { jobCloudGetAccount ->
                GetAccountListResult(
                    data = jobCloudGetAccount.data?.map {
                        AuthorizedAccount(
                            id = it.id,
                            account = it.account,
                            type = it.type,
                            category = it.category,
                            alias = it.alias,
                            os = it.os,
                            description = it.description,
                            creator = it.creator,
                            createTime = it.createTime,
                            lastModifyUser = it.lastModifyUser,
                            lastModifyTime = it.lastModifyTime,
                            dbSystemAccountId = it.dbSystemAccountId,
                            bkBizId = it.bkBizId,
                            bkScopeType = it.bkScopeType,
                            bkScopeId = it.bkScopeId
                        )
                    },
                    start = jobCloudGetAccount.start,
                    total = jobCloudGetAccount.total,
                    length = jobCloudGetAccount.length
                )
            }
        )
        return getAccountListRes
    }
}