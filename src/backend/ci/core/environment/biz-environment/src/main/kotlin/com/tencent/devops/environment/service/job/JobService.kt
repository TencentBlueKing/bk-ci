package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.environment.pojo.job.agentreq.AgentCondition
import com.tencent.devops.environment.pojo.job.agentreq.AgentHostForInstallAgent
import com.tencent.devops.environment.pojo.job.agentreq.AgentInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentTaskLogResult
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentRetryAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentTerminalAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.Condition
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
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudAccountAlias
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudCreateAccountReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudDeleteAccountReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudExecuteTarget
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudFileSource
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudTaskTerminateReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudCreateAccountResult
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudGetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudGetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.agentreq.InstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskLogResult
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.RetryAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.TerminalAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentres.AgentInfo
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.resp.Account
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.AgentRetryAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.AgentTerminalAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.FilterHostInfo
import com.tencent.devops.environment.pojo.job.agentres.HostDetail
import com.tencent.devops.environment.pojo.job.resp.ApprovalStepInfo
import com.tencent.devops.environment.pojo.job.resp.AuthorizedAccount
import com.tencent.devops.environment.pojo.job.resp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.resp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.resp.DynamicGroup
import com.tencent.devops.environment.pojo.job.resp.FileDestination
import com.tencent.devops.environment.pojo.job.resp.FileDistributeLog
import com.tencent.devops.environment.pojo.job.resp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.resp.FileLog
import com.tencent.devops.environment.pojo.job.resp.FileSource
import com.tencent.devops.environment.pojo.job.resp.FileStepInfo
import com.tencent.devops.environment.pojo.job.resp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.HostInRes
import com.tencent.devops.environment.pojo.job.resp.HostIpv6
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.IpFilter
import com.tencent.devops.environment.pojo.job.agentres.Meta
import com.tencent.devops.environment.pojo.job.resp.JobInstance
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.JobStepInstance
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.RetryAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.Statistics
import com.tencent.devops.environment.pojo.job.agentres.TerminalAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudIpInfo
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.ScriptExcuteLog
import com.tencent.devops.environment.pojo.job.resp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.resp.ScriptStepInfo
import com.tencent.devops.environment.pojo.job.resp.StepHostResult
import com.tencent.devops.environment.pojo.job.resp.StepHostResultForGetStepInstanceStatus
import com.tencent.devops.environment.pojo.job.resp.StepResultGroup
import com.tencent.devops.environment.pojo.job.resp.TaskTerminateResult
import com.tencent.devops.environment.pojo.job.resp.TopoNode
import com.tencent.devops.environment.pojo.job.resp.VariableServer
import com.tencent.devops.environment.service.job.api.ApigwJobCloudApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("JobService")
class JobService @Autowired constructor(
    private val apigwJobCloudApi: ApigwJobCloudApi,
    private val agentApi: AgentApi,
    private val parseHashListService: ParseHashListService,
    private val permissionManageService: PermissionManageService
) {
    companion object {
        private const val JOB_TYPE_INSTALL_AGENT = "INSTALL_AGENT"
    }

    fun executeScript(
        projectId: String,
        userId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult> {
        val allHostList: List<Host> = parseHashListService.getAllHostList(projectId, scriptExecuteReq.executeTarget)
        permissionManageService.isUserHasAllPermission(userId, projectId, allHostList)
        val jobCloudScriptExecuteReq = JobCloudScriptExecuteReq(
            scriptContent = scriptExecuteReq.scriptContent,
            scriptParam = scriptExecuteReq.scriptParam,
            timeout = scriptExecuteReq.timeout,
            accountAlias = scriptExecuteReq.accountAlias,
            accountId = scriptExecuteReq.accountId,
            isParamSensitive = scriptExecuteReq.isSensiveParam,
            scriptLanguage = scriptExecuteReq.scriptLanguage,
            targetServer = JobCloudExecuteTarget(
                hostIdList = allHostList.filter { it.bkHostId != null }.map {
                    it.bkHostId ?: 0L
                },
                ipList = allHostList.filter { it.bkHostId == null }.map {
                    JobCloudIpInfo(
                        bkCloudId = it.bkCloudId ?: 0,
                        ip = it.ip
                    )
                }
            ),
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setThreadLocal(::executeScript.name)

        val jobCloudScriptExecuteRes: JobCloudResult<JobCloudScriptExecuteResult> = apigwJobCloudApi.executePostRequest(
            jobCloudScriptExecuteReq, JobCloudScriptExecuteResult::class.java
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
        projectId: String,
        userId: String,
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult> {
        val allExecuteTargetHostList: List<Host> = parseHashListService.getAllHostList(
            projectId, fileDistributeReq.executeTarget
        )
        permissionManageService.isUserHasAllPermission(userId, projectId, allExecuteTargetHostList)
        val jobCloudFileDistributeReq = JobCloudFileDistributeReq(
            fileSourceList = fileDistributeReq.fileSourceList.map { fileSource ->
                val allFileSourceHostList: List<Host> = parseHashListService.getAllHostList(
                    projectId, fileSource.sourceFileServer
                )
                permissionManageService.isUserHasAllPermission(userId, projectId, allFileSourceHostList)
                JobCloudFileSource(
                    fileList = fileSource.fileList.toList(),
                    server = JobCloudExecuteTarget(
                        ipList = allFileSourceHostList.filter { it.bkHostId == null }.map {
                            JobCloudIpInfo(bkCloudId = it.bkCloudId ?: 0, ip = it.ip)
                        },
                        hostIdList = allFileSourceHostList.filter { it.bkHostId != null }.map { it.bkHostId ?: 0L }
                    ),
                    account = JobCloudAccountAlias(
                        id = fileSource.account.id,
                        alias = fileSource.account.alias
                    )
                )
            },
            fileTargetPath = fileDistributeReq.fileTargetPath,
            transferMode = fileDistributeReq.transferMode,
            executeTarget = JobCloudExecuteTarget(
                ipList = allExecuteTargetHostList.filter { it.bkHostId == null }.map {
                    JobCloudIpInfo(bkCloudId = it.bkCloudId ?: 0, ip = it.ip)
                },
                hostIdList = allExecuteTargetHostList.filter { it.bkHostId != null }.map { it.bkHostId ?: 0L }
            ),
            accountAlias = fileDistributeReq.accountAlias,
            accountId = fileDistributeReq.accountId,
            timeout = fileDistributeReq.timeout,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setThreadLocal(::distributeFile.name)

        val jobCloudFileDistributeRes: JobCloudResult<JobCloudFileDistributeResult> =
            apigwJobCloudApi.executePostRequest(
                jobCloudFileDistributeReq, JobCloudFileDistributeResult::class.java
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

    fun terminateTask(taskTerminateReq: TaskTerminateReq): JobResult<TaskTerminateResult> {
        val jobCloudTaskTerminateReq = JobCloudTaskTerminateReq(
            jobInstanceId = taskTerminateReq.jobInstanceId,
            operationCode = taskTerminateReq.operationCode,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setThreadLocal(::terminateTask.name)

        val jobCloudTaskTerminateRes: JobCloudResult<JobCloudTaskTerminateResult> = apigwJobCloudApi.executePostRequest(
            jobCloudTaskTerminateReq, JobCloudTaskTerminateResult::class.java
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
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        val jobCloudQueryJobInstanceLogsReq = JobCloudQueryJobInstanceLogsReq(
            jobInstanceId = queryJobInstanceLogsReq.jobInstanceId,
            stepInstanceId = queryJobInstanceLogsReq.stepInstanceId,
            ipList = queryJobInstanceLogsReq.ipList?.map {
                JobCloudIpInfo(bkCloudId = it.bkCloudId ?: 0, ip = it.ip)
            },
            hostIdList = queryJobInstanceLogsReq.hostIdList,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setThreadLocal(::queryJobInstanceLogs.name)
        val jobCloudQueryJobInstanceLogsRes: JobCloudResult<JobCloudQueryJobInstanceLogsResult> =
            apigwJobCloudApi.executePostRequest(
                jobCloudQueryJobInstanceLogsReq, JobCloudQueryJobInstanceLogsResult::class.java
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
                                            ipv6 = srcHost.ipv6, bkHostId = srcHost.bkHostId
                                        )
                                    },
                                    srcPath = jobCloudFileLog.srcPath,
                                    destHost = jobCloudFileLog.destHost?.let { destHost ->
                                        HostInRes(
                                            bkCloudId = destHost.bkCloudId, ip = destHost.ip,
                                            ipv6 = destHost.ipv6, bkHostId = destHost.bkHostId
                                        )
                                    },
                                    destPath = jobCloudFileLog.destPath,
                                    status = jobCloudFileLog.status,
                                    logContent = jobCloudFileLog.logContent,
                                    size = jobCloudFileLog.size,
                                    speed = jobCloudFileLog.speed,
                                    process = jobCloudFileLog.process
                                )
                            }
                        )
                    }
                )
            }
        )
        return queryJobInstanceLogsRes
    }

    fun createAccount(createAccountReq: CreateAccountReq): JobResult<CreateAccountResult> {
        val jobCloudCreateAccountReq = JobCloudCreateAccountReq(
            account = createAccountReq.account,
            type = createAccountReq.type,
            category = createAccountReq.category,
            password = createAccountReq.password,
            alias = createAccountReq.alias,
            description = createAccountReq.description,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setThreadLocal(::createAccount.name)

        val jobCloudCreateAccountRes: JobCloudResult<JobCloudCreateAccountResult> = apigwJobCloudApi.executePostRequest(
            jobCloudCreateAccountReq, JobCloudCreateAccountResult::class.java
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

    fun deleteAccount(deleteAccountReq: DeleteAccountReq): JobResult<DeleteAccountResult> {
        val jobCloudDeleteAccountReq = JobCloudDeleteAccountReq(
            id = deleteAccountReq.id,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setThreadLocal(::deleteAccount.name)

        val jobCloudDeleteAccountRes: JobCloudResult<JobCloudDeleteAccountResult> = apigwJobCloudApi.executePostRequest(
            jobCloudDeleteAccountReq, JobCloudDeleteAccountResult::class.java
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
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        ApigwJobCloudApi.setThreadLocal("queryJobInstanceStatus")
        val jobCloudQueryJobInstanceStatusRes: JobCloudResult<JobCloudQueryJobInstanceStatusResult> =
            apigwJobCloudApi.executeGetRequest(
                JobCloudQueryJobInstanceStatusResult::class.java, jobInstanceId, returnIpResult ?: ""
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
                            stepIpResultList = stepInstance.jobCloudStepHostResultList?.map { jobCldStepHostResult ->
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
                JobCloudGetAccountListResult::class.java,
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

    fun getStepInstanceDetail(
        projectId: String,
        jobInstanceId: Long,
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult> {
        ApigwJobCloudApi.setThreadLocal("getStepInstanceDetail")
        val jobCloudGetStepInstanceDetailRes: JobCloudResult<JobCloudGetStepInstanceDetailResult> =
            apigwJobCloudApi.executeGetRequest(
                JobCloudGetStepInstanceDetailResult::class.java, jobInstanceId, stepInstanceId
            )
        val getStepInstanceDetailRes: JobResult<GetStepInstanceDetailResult> = JobResult(
            code = jobCloudGetStepInstanceDetailRes.code,
            result = jobCloudGetStepInstanceDetailRes.result,
            jobRequestId = jobCloudGetStepInstanceDetailRes.jobRequestId,
            data = jobCloudGetStepInstanceDetailRes.data?.let { jobCloudGetStepInstanceDetail ->
                GetStepInstanceDetailResult(
                    id = jobCloudGetStepInstanceDetail.id,
                    type = jobCloudGetStepInstanceDetail.type,
                    name = jobCloudGetStepInstanceDetail.name,
                    scriptStepInfo = jobCloudGetStepInstanceDetail.jobCloudScriptStepInfo
                        ?.let { jobCloudScriptStepInfo ->
                            ScriptStepInfo(
                                scriptType = jobCloudScriptStepInfo.scriptType,
                                scriptId = jobCloudScriptStepInfo.scriptId,
                                scriptVersionId = jobCloudScriptStepInfo.scriptVersionId,
                                scriptContent = jobCloudScriptStepInfo.scriptContent,
                                scriptLanguage = jobCloudScriptStepInfo.scriptLanguage,
                                scriptParam = jobCloudScriptStepInfo.scriptParam,
                                scriptTimeout = jobCloudScriptStepInfo.scriptTimeout,
                                account = jobCloudScriptStepInfo.account.let { jobCloudAccount ->
                                    Account(id = jobCloudAccount.id, name = jobCloudAccount.name)
                                },
                                server = jobCloudScriptStepInfo.server.let { jobCloudVariableServer ->
                                    VariableServer(
                                        variable = jobCloudVariableServer.variable,
                                        hostList = jobCloudVariableServer.jobCloudHostList?.map {
                                            HostIpv6(
                                                bkHostId = it.bkHostId, bkCloudId = it.bkCloudId,
                                                ip = it.ip, ipv6 = it.ipv6
                                            )
                                        },
                                        topoNodeList = jobCloudVariableServer.jobCloudTopoNodeList?.map {
                                            TopoNode(nodeType = it.nodeType, id = it.id)
                                        },
                                        dynamicGroupList = jobCloudVariableServer.jobCloudDynamicGroupList?.map {
                                            DynamicGroup(id = it.id)
                                        }
                                    )
                                },
                                isParamSensitive = jobCloudScriptStepInfo.isParamSensitive,
                                isIgnoreError = jobCloudScriptStepInfo.isIgnoreError
                            )
                        },
                    fileStepInfo = jobCloudGetStepInstanceDetail.jobCloudFileStepInfo?.let { jobCloudFileStepInfo ->
                        FileStepInfo(
                            fileSourceList = jobCloudFileStepInfo.jobCloudFileSourceList.map { jobCloudFileSource ->
                                FileSource(
                                    fileType = jobCloudFileSource.fileType,
                                    fileList = jobCloudFileSource.fileList,
                                    server = jobCloudFileSource.server.let { jobCloudVariableServer ->
                                        VariableServer(
                                            variable = jobCloudVariableServer?.variable,
                                            hostList = jobCloudVariableServer?.jobCloudHostList?.map {
                                                HostIpv6(
                                                    bkHostId = it.bkHostId, bkCloudId = it.bkCloudId,
                                                    ip = it.ip, ipv6 = it.ipv6
                                                )
                                            },
                                            topoNodeList = jobCloudVariableServer?.jobCloudTopoNodeList?.map {
                                                TopoNode(nodeType = it.nodeType, id = it.id)
                                            },
                                            dynamicGroupList = jobCloudVariableServer?.jobCloudDynamicGroupList?.map {
                                                DynamicGroup(id = it.id)
                                            }
                                        )
                                    },
                                    account = jobCloudFileSource.account.let { jobCloudAccount ->
                                        Account(id = jobCloudAccount.id, name = jobCloudAccount.name)
                                    },
                                    fileSourceId = jobCloudFileSource.fileSourceId
                                )
                            },
                            fileDestination = jobCloudFileStepInfo.fileDestination.let { jobCloudFileDestination ->
                                FileDestination(
                                    path = jobCloudFileDestination.path,
                                    account = jobCloudFileDestination.account.let { jobCloudAccount ->
                                        Account(id = jobCloudAccount.id, name = jobCloudAccount.name)
                                    },
                                    server = jobCloudFileDestination.server.let { jobCloudVariableServer ->
                                        VariableServer(
                                            variable = jobCloudVariableServer.variable,
                                            hostList = jobCloudVariableServer.jobCloudHostList?.map {
                                                HostIpv6(
                                                    bkHostId = it.bkHostId, bkCloudId = it.bkCloudId,
                                                    ip = it.ip, ipv6 = it.ipv6
                                                )
                                            },
                                            topoNodeList = jobCloudVariableServer.jobCloudTopoNodeList?.map {
                                                TopoNode(nodeType = it.nodeType, id = it.id)
                                            },
                                            dynamicGroupList = jobCloudVariableServer.jobCloudDynamicGroupList?.map {
                                                DynamicGroup(id = it.id)
                                            }
                                        )
                                    }
                                )
                            },
                            timeout = jobCloudFileStepInfo.timeout,
                            sourceSpeedLimit = jobCloudFileStepInfo.sourceSpeedLimit,
                            destinationSpeedLimit = jobCloudFileStepInfo.destinationSpeedLimit,
                            transferMode = jobCloudFileStepInfo.transferMode,
                            isIgnoreError = jobCloudFileStepInfo.isIgnoreError
                        )
                    },
                    approvalStepInfo = jobCloudGetStepInstanceDetail.jobCloudApprovalStepInfo?.let {
                        ApprovalStepInfo(approvalMessage = it.approvalMessage)
                    }
                )
            }
        )
        return getStepInstanceDetailRes
    }

    fun getStepInstanceStatus(
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
        ApigwJobCloudApi.setThreadLocal("getStepInstanceStatus")
        val jobCloudGetStepInstanceStatusRes: JobCloudResult<JobCloudGetStepInstanceStatusResult> =
            apigwJobCloudApi.executeGetRequest(
                JobCloudGetStepInstanceStatusResult::class.java,
                jobInstanceId, stepInstanceId, executeCount ?: "", batch ?: "",
                maxHostNumPerGroup ?: "", keyword ?: "", searchIp ?: "", status ?: "", tag ?: ""
            )
        val getStepInstanceStatusRes: JobResult<GetStepInstanceStatusResult> = JobResult(
            code = jobCloudGetStepInstanceStatusRes.code,
            result = jobCloudGetStepInstanceStatusRes.result,
            jobRequestId = jobCloudGetStepInstanceStatusRes.jobRequestId,
            data = jobCloudGetStepInstanceStatusRes.data?.let { jobCloudGetStepInstanceStatus ->
                GetStepInstanceStatusResult(
                    status = jobCloudGetStepInstanceStatus.status,
                    totalTime = jobCloudGetStepInstanceStatus.totalTime,
                    name = jobCloudGetStepInstanceStatus.name,
                    stepInstanceId = jobCloudGetStepInstanceStatus.stepInstanceId,
                    executeCount = jobCloudGetStepInstanceStatus.status,
                    createTime = jobCloudGetStepInstanceStatus.createTime,
                    endTime = jobCloudGetStepInstanceStatus.endTime,
                    type = jobCloudGetStepInstanceStatus.type,
                    startTime = jobCloudGetStepInstanceStatus.startTime,
                    stepResultGroupList = jobCloudGetStepInstanceStatus.stepResultGroupList?.map { stepResultGroup ->
                        StepResultGroup(
                            resultType = stepResultGroup.resultType,
                            resultTypeDesc = stepResultGroup.resultTypeDesc,
                            tag = stepResultGroup.tag,
                            hostSize = stepResultGroup.hostSize,
                            hostResultList = stepResultGroup.hostResultList.map { stepHostResult ->
                                StepHostResultForGetStepInstanceStatus(
                                    bkHostId = stepHostResult.bkHostId,
                                    ip = stepHostResult.ip,
                                    ipv6 = stepHostResult.ipv6,
                                    bkCloudId = stepHostResult.bkCloudId,
                                    bkAgentId = stepHostResult.bkAgentId,
                                    bkCloudName = stepHostResult.bkCloudName,
                                    status = stepHostResult.status,
                                    statusDesc = stepHostResult.statusDesc,
                                    tag = stepHostResult.tag,
                                    exitCode = stepHostResult.exitCode,
                                    startTime = stepHostResult.startTime,
                                    endTime = stepHostResult.endTime,
                                    totalTime = stepHostResult.totalTime
                                )
                            }
                        )
                    }
                )
            }
        )
        return getStepInstanceStatusRes
    }

    fun installAgent(
        userId: String,
        projectId: String,
        installAgentReq: InstallAgentReq
    ): AgentResult<InstallAgentResult> {
        AgentApi.setThreadLocal("installAgent")
        val installAgentRequest = AgentInstallAgentReq(
            jobType = JOB_TYPE_INSTALL_AGENT,
            hosts = installAgentReq.hosts.map {
                AgentHostForInstallAgent(
                    bkBizId = it.bkBizId,
                    bkCloudId = it.bkCloudId,
                    bkHostId = it.bkHostId,
                    bkAddressing = it.bkAddressing,
                    apId = it.apId,
                    installChannelId = it.installChannelId,
                    innerIp = it.innerIp,
                    outerIp = it.outerIp,
                    loginIp = it.loginIp,
                    dataIp = it.dataIp,
                    innerIpv6 = it.innerIpv6,
                    outerIpv6 = it.outerIpv6,
                    osType = it.osType,
                    authType = it.authType,
                    account = it.account,
                    password = it.password,
                    port = it.port,
                    key = it.key,
                    isManual = it.isManual,
                    retention = it.retention,
                    peerExchangeSwitchForAgent = it.peerExchangeSwitchForAgent,
                    btSpeedLimit = it.btSpeedLimit,
                    enableCompression = it.enableCompression,
                    dataPath = it.dataPath
                )
            },
            replaceHostId = installAgentReq.replaceHostId,
            isInstallLatestPlugins = installAgentReq.isInstallLatestPlugins
        )
        val agentInstallAgentRes: AgentResult<AgentInstallAgentResult> = agentApi.executePostRequest(
            installAgentRequest, AgentInstallAgentResult::class.java
        )
        val installAgentRes: AgentResult<InstallAgentResult> = AgentResult(
            code = agentInstallAgentRes.code,
            result = agentInstallAgentRes.result,
            message = agentInstallAgentRes.message,
            errors = agentInstallAgentRes.errors,
            data = InstallAgentResult(
                jobId = agentInstallAgentRes.data?.jobId,
                jobUrl = agentInstallAgentRes.data?.jobUrl,
                ipFilter = agentInstallAgentRes.data?.ipFilter?.map {
                    IpFilter(
                        bkBizId = it.bkBizId,
                        bkBizName = it.bkBizName,
                        ip = it.ip,
                        innerIp = it.innerIp,
                        innerIpv6 = it.innerIpv6,
                        bkHostId = it.bkHostId,
                        bkCloudName = it.bkCloudName,
                        bkCloudId = it.bkCloudId,
                        status = it.status,
                        jobId = it.jobId,
                        exception = it.exception,
                        msg = it.msg
                    )
                }
            )
        )
        return installAgentRes
    }

    fun queryAgentTaskStatus(
        userId: String,
        projectId: String,
        jobId: Int,
        queryAgentTaskStatusReq: QueryAgentTaskStatusReq
    ): AgentResult<QueryAgentTaskStatusResult> {
        AgentApi.setThreadLocal("queryAgentTaskStatus")
        val queryAgentTaskStatusRequest = AgentQueryAgentTaskStatusReq(
            conditions = queryAgentTaskStatusReq.conditions?.map {
                AgentCondition(
                    key = it.key,
                    value = it.value
                )
            },
            page = queryAgentTaskStatusReq.page,
            pageSize = queryAgentTaskStatusReq.pageSize
        )
        val agentQueryAgentTaskStatusRes: AgentResult<AgentQueryAgentTaskStatusResult> = agentApi.executePostRequest(
            queryAgentTaskStatusRequest, AgentQueryAgentTaskStatusResult::class.java, jobId
        )
        val queryAgentTaskStatusRes: AgentResult<QueryAgentTaskStatusResult> = AgentResult(
            code = agentQueryAgentTaskStatusRes.code,
            result = agentQueryAgentTaskStatusRes.result,
            message = agentQueryAgentTaskStatusRes.message,
            errors = agentQueryAgentTaskStatusRes.errors,
            data = agentQueryAgentTaskStatusRes.data?.let {
                QueryAgentTaskStatusResult(
                    jobId = it.jobId,
                    createdBy = it.createdBy,
                    jobType = it.jobType,
                    jobTypeDisplay = it.jobTypeDisplay,
                    ipFilterList = it.ipFilterList,
                    total = it.total,
                    list = it.list?.map { hostDetail ->
                        HostDetail(
                            filterHost = hostDetail.filterHost,
                            bkHostId = hostDetail.bkHostId,
                            ip = hostDetail.ip,
                            innerIp = hostDetail.innerIp,
                            innerIpv6 = hostDetail.innerIpv6,
                            bkCloudId = hostDetail.bkCloudId,
                            bkCloudName = hostDetail.bkCloudName,
                            bkBizId = hostDetail.bkBizId,
                            bkBizName = hostDetail.bkBizName,
                            jobId = hostDetail.jobId,
                            status = hostDetail.status,
                            statusDisplay = hostDetail.statusDisplay
                        )
                    },
                    statistics = it.statistics.let { statistics ->
                        Statistics(
                            totalCount = statistics.totalCount,
                            failedCount = statistics.failedCount,
                            ignoredCount = statistics.ignoredCount,
                            pendingCount = statistics.pendingCount,
                            runningCount = statistics.runningCount,
                            successCount = statistics.successCount
                        )
                    },
                    status = it.status,
                    endTime = it.endTime,
                    startTime = it.startTime,
                    costTime = it.costTime,
                    meta = it.meta.let { meta ->
                        Meta(
                            type = meta.type,
                            stepType = meta.stepType,
                            opType = meta.opType,
                            opTypeDisplay = meta.opTypeDisplay,
                            stepTypeDisplay = meta.stepTypeDisplay,
                            name = meta.name,
                            category = meta.category,
                            pluginName = meta.pluginName
                        )
                    }
                )
            }
        )
        return queryAgentTaskStatusRes
    }

    fun queryAgentStatusFromJob(
        userId: String,
        projectId: String,
        queryAgentStatusFromJobReq: QueryAgentStatusFromJobReq
    ): AgentResult<QueryAgentStatusFromJobResult> {
        AgentApi.setThreadLocal("queryAgentStatus")
        val queryAgentStatusFromJobRequest = AgentQueryAgentStatusFromJobReq(
            hostIdList = queryAgentStatusFromJobReq.hostIdList
        )
        val agentQueryAgentStatusFromJobRes: AgentResult<AgentQueryAgentStatusFromJobResult> =
            agentApi.executePostRequest(
                queryAgentStatusFromJobRequest, AgentQueryAgentStatusFromJobResult::class.java
            )
        val queryAgentStatusFromJobRes: AgentResult<QueryAgentStatusFromJobResult> = AgentResult(
            code = agentQueryAgentStatusFromJobRes.code,
            result = agentQueryAgentStatusFromJobRes.result,
            message = agentQueryAgentStatusFromJobRes.message,
            errors = agentQueryAgentStatusFromJobRes.errors,
            data = agentQueryAgentStatusFromJobRes.data?.let {
                QueryAgentStatusFromJobResult(
                    agentInfoList = it.agentInfoList.map { agentInfo ->
                        AgentInfo(
                            bkHostId = agentInfo.bkHostId,
                            status = agentInfo.status,
                            version = agentInfo.version
                        )
                    }
                )
            }
        )
        return queryAgentStatusFromJobRes
    }

    fun queryAgentStatusFromNodeman(
        userId: String,
        projectId: String,
        queryAgentStatusFromNodemanReq: QueryAgentStatusFromNodemanReq
    ): AgentResult<QueryAgentStatusFromNodemanResult> {
        AgentApi.setThreadLocal("queryAgentStatusFromNodeman")
        val queryAgentStatusFromNodemanRequest = AgentQueryAgentStatusFromNodemanReq(
            bkHostId = queryAgentStatusFromNodemanReq.bkHostId,
            conditions = queryAgentStatusFromNodemanReq.conditions?.map {
                AgentCondition(key = it.key, value = it.value)
            },
            extraData = queryAgentStatusFromNodemanReq.extraData,
            pagesize = queryAgentStatusFromNodemanReq.pagesize,
            page = queryAgentStatusFromNodemanReq.page,
            onlyIp = queryAgentStatusFromNodemanReq.onlyIp,
            runningCount = queryAgentStatusFromNodemanReq.runningCount
        )
        val agentQueryAgentStatusRes: AgentResult<AgentQueryAgentStatusFromNodemanResult> = agentApi.executePostRequest(
            queryAgentStatusFromNodemanRequest, AgentQueryAgentStatusFromNodemanResult::class.java
        )
        val queryAgentStatusRes: AgentResult<QueryAgentStatusFromNodemanResult> = AgentResult(
            code = agentQueryAgentStatusRes.code,
            result = agentQueryAgentStatusRes.result,
            message = agentQueryAgentStatusRes.message,
            errors = agentQueryAgentStatusRes.errors,
            data = agentQueryAgentStatusRes.data?.let {
                QueryAgentStatusFromNodemanResult(
                    total = it.total,
                    list = it.list.map { filterHostInfo->
                        FilterHostInfo(
                            filterHost=filterHostInfo.filterHost,
                            bkHostId=filterHostInfo.bkHostId,
                            ip=filterHostInfo.ip,
                            innerIp=filterHostInfo.innerIp,
                            innerIpv6=filterHostInfo.innerIpv6,
                            bkCloudId=filterHostInfo.bkCloudId,
                            bkCloudName=filterHostInfo.bkCloudName,
                            bkBizId=filterHostInfo.bkBizId,
                            bkBizName=filterHostInfo.bkBizName,
                            jobId=filterHostInfo.jobId,
                            status=filterHostInfo.status,
                            statusDisplay=filterHostInfo.statusDisplay
                        )
                    }
                )
            }
        )
        return queryAgentStatusRes
    }

    fun queryAgentTaskLog(
        userId: String,
        projectId: String,
        jobId: Int,
        instanceId: Long
    ): AgentResult<QueryAgentTaskLogResult> {
        AgentApi.setThreadLocal("queryAgentTaskLog")
        val agentQueryAgentTaskLogRes: AgentResult<AgentQueryAgentTaskLogResult> = agentApi.executeGetRequest(
            AgentQueryAgentTaskLogResult::class.java, jobId, instanceId
        )
        val queryAgentTaskLogRes: AgentResult<QueryAgentTaskLogResult> = AgentResult(
            code = agentQueryAgentTaskLogRes.code,
            result = agentQueryAgentTaskLogRes.result,
            message = agentQueryAgentTaskLogRes.message,
            errors = agentQueryAgentTaskLogRes.errors,
            data = agentQueryAgentTaskLogRes.data?.let {
                QueryAgentTaskLogResult(
                    step = it.step,
                    status = it.status,
                    log = it.log,
                    startTime = it.startTime,
                    finishTime = it.finishTime
                )
            }
        )
        return queryAgentTaskLogRes
    }

    fun terminalAgentInstallTask(
        userId: String,
        projectId: String,
        jobId: Int,
        terminalAgentInstallTaskReq: TerminalAgentInstallTaskReq
    ): AgentResult<TerminalAgentInstallTaskResult> {
        AgentApi.setThreadLocal("terminalAgentInstallTask")
        val terminalAgentInstallTaskRequest = AgentTerminalAgentInstallTaskReq(
            instanceIdList = terminalAgentInstallTaskReq.instanceIdList
        )
        val agentTrmAgentInstallTaskRes: AgentResult<AgentTerminalAgentInstallTaskResult> = agentApi.executePostRequest(
            terminalAgentInstallTaskRequest, AgentTerminalAgentInstallTaskResult::class.java, jobId
        )
        val termAgentInstallTaskRes: AgentResult<TerminalAgentInstallTaskResult> = AgentResult(
            code = agentTrmAgentInstallTaskRes.code,
            result = agentTrmAgentInstallTaskRes.result,
            message = agentTrmAgentInstallTaskRes.message,
            errors = agentTrmAgentInstallTaskRes.errors,
            data = agentTrmAgentInstallTaskRes.data?.let {
                TerminalAgentInstallTaskResult(
                    taskIdList = it.taskIdList
                )
            }
        )
        return termAgentInstallTaskRes
    }

    fun retryAgentInstallTask(
        userId: String,
        projectId: String,
        jobId: Int,
        retryAgentInstallTaskReq: RetryAgentInstallTaskReq
    ): AgentResult<RetryAgentInstallTaskResult> {
        AgentApi.setThreadLocal("retryAgentInstallTask")
        val retryAgentInstallTaskRequest = AgentRetryAgentInstallTaskReq(
            instanceIdList = retryAgentInstallTaskReq.instanceIdList
        )
        val agentRetryAgentInstallTaskRes: AgentResult<AgentRetryAgentInstallTaskResult> = agentApi.executePostRequest(
            retryAgentInstallTaskRequest, AgentRetryAgentInstallTaskResult::class.java, jobId
        )
        val retryAgentInstallTaskRes: AgentResult<RetryAgentInstallTaskResult> = AgentResult(
            code = agentRetryAgentInstallTaskRes.code,
            result = agentRetryAgentInstallTaskRes.result,
            message = agentRetryAgentInstallTaskRes.message,
            errors = agentRetryAgentInstallTaskRes.errors,
            data = agentRetryAgentInstallTaskRes.data?.let {
                RetryAgentInstallTaskResult(
                    taskIdList = it.taskIdList
                )
            }
        )
        return retryAgentInstallTaskRes
    }
}