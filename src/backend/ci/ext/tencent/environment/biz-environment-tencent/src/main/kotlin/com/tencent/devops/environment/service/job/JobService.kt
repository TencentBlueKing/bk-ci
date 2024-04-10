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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudQueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.jobreq.CreateAccountReq
import com.tencent.devops.environment.pojo.job.jobreq.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudDeleteAccountResult
import com.tencent.devops.environment.pojo.job.jobreq.FileDistributeReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudFileDistributeResult
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudGetAccountListResult
import com.tencent.devops.environment.pojo.job.jobreq.Host
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudResult
import com.tencent.devops.environment.pojo.job.jobreq.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudQueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudQueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobreq.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudScriptExecuteResult
import com.tencent.devops.environment.pojo.job.jobreq.TaskTerminateReq
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
import com.tencent.devops.environment.pojo.job.jobreq.QueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudQueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.jobresp.Account
import com.tencent.devops.environment.pojo.job.jobresp.AgentInfo
import com.tencent.devops.environment.pojo.job.jobresp.ApprovalStepInfo
import com.tencent.devops.environment.pojo.job.jobresp.AuthorizedAccount
import com.tencent.devops.environment.pojo.job.jobresp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.jobresp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.jobresp.DynamicGroup
import com.tencent.devops.environment.pojo.job.jobresp.FileDestination
import com.tencent.devops.environment.pojo.job.jobresp.FileDistributeLog
import com.tencent.devops.environment.pojo.job.jobresp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.jobresp.FileLog
import com.tencent.devops.environment.pojo.job.jobresp.FileSource
import com.tencent.devops.environment.pojo.job.jobresp.FileStepInfo
import com.tencent.devops.environment.pojo.job.jobresp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.HostInRes
import com.tencent.devops.environment.pojo.job.jobresp.HostIpv6
import com.tencent.devops.environment.pojo.job.jobresp.JobInstance
import com.tencent.devops.environment.pojo.job.jobresp.JobResult
import com.tencent.devops.environment.pojo.job.jobresp.JobStepInstance
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudIpInfo
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudHostInRes
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudVariableServer
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.ScriptExcuteLog
import com.tencent.devops.environment.pojo.job.jobresp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.jobresp.ScriptStepInfo
import com.tencent.devops.environment.pojo.job.jobresp.StepHostResult
import com.tencent.devops.environment.pojo.job.jobresp.StepHostResultForGetStepInstanceStatus
import com.tencent.devops.environment.pojo.job.jobresp.StepResultGroup
import com.tencent.devops.environment.pojo.job.jobresp.TaskTerminateResult
import com.tencent.devops.environment.pojo.job.jobresp.TopoNode
import com.tencent.devops.environment.pojo.job.jobresp.VariableServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service("JobService")
class JobService @Autowired constructor(
    private val apigwJobCloudApi: ApigwJobCloudApi,
    private val parseHashListService: ParseHashListService,
    private val permissionManageService: PermissionManageService
) {
    fun executeScript(
        projectId: String,
        userId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult> {
        val allHostList: List<Host>? = scriptExecuteReq.executeTarget?.let {
            parseHashListService.getAllHostList(projectId, it)
        }
        if (allHostList.isNullOrEmpty()) {
            throw CustomException(
                Response.Status.BAD_REQUEST,
                "Host is empty."
            )
        }
        if (allHostList.isNotEmpty()) {
            permissionManageService.isUserHasAllPermission(userId, projectId, allHostList)
        }
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
                        bkCloudId = it.bkCloudId,
                        ip = it.ip
                    )
                }
            ),
            taskName = scriptExecuteReq.taskName,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setJobOperationName(::executeScript.name)

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
        val allFileSourceHostList: MutableList<Host> = mutableListOf()
        fileDistributeReq.fileSourceList.map { fileSource ->
            val fileSourceHostList = parseHashListService.getAllHostList(projectId, fileSource.sourceFileServer)
            for (fileSourceHost in fileSourceHostList) {
                allFileSourceHostList.add(fileSourceHost)
            }
        }
        if (allExecuteTargetHostList.isEmpty()) {
            throw CustomException(
                Response.Status.BAD_REQUEST,
                "Execute target host is empty."
            )
        }
        if (allFileSourceHostList.isEmpty()) {
            throw CustomException(
                Response.Status.BAD_REQUEST,
                "File source host is empty."
            )
        }
        val allHostList = allExecuteTargetHostList.plus(allFileSourceHostList)
        permissionManageService.isUserHasAllPermission(userId, projectId, allHostList)
        val jobCloudFileDistributeReq = JobCloudFileDistributeReq(
            fileSourceList = fileDistributeReq.fileSourceList.map { fileSource ->
                val fileSourceHostList: List<Host> = parseHashListService.getAllHostList(
                    projectId, fileSource.sourceFileServer
                )
                JobCloudFileSource(
                    fileList = fileSource.fileList.toList(),
                    server = JobCloudExecuteTarget(
                        ipList = fileSourceHostList.filter { it.bkHostId == null }.map {
                            JobCloudIpInfo(bkCloudId = it.bkCloudId, ip = it.ip)
                        },
                        hostIdList = fileSourceHostList.filter { it.bkHostId != null }.map { it.bkHostId ?: 0L }
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
                    JobCloudIpInfo(bkCloudId = it.bkCloudId, ip = it.ip)
                },
                hostIdList = allExecuteTargetHostList.filter { it.bkHostId != null }.map { it.bkHostId ?: 0L }
            ),
            accountAlias = fileDistributeReq.accountAlias,
            accountId = fileDistributeReq.accountId,
            timeout = fileDistributeReq.timeout,
            taskName = fileDistributeReq.taskName,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setJobOperationName(::distributeFile.name)

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
        ApigwJobCloudApi.setJobOperationName(::terminateTask.name)

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
                JobCloudIpInfo(bkCloudId = it.bkCloudId, ip = it.ip)
            },
            hostIdList = queryJobInstanceLogsReq.hostIdList,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        ApigwJobCloudApi.setJobOperationName(::queryJobInstanceLogs.name)
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
                    jobInstanceId = it.jobInstanceId,
                    stepInstanceId = it.stepInstanceId,
                    logType = it.logType,
                    scriptTaskLogs = it.scriptTaskLogs?.map { scriptTaskLog ->
                        ScriptExcuteLog(
                            ip = scriptTaskLog.ip,
                            ipv6 = scriptTaskLog.ipv6,
                            bkHostId = scriptTaskLog.bkHostId,
                            bkCloudId = scriptTaskLog.bkCloudId,
                            logContent = scriptTaskLog.logContent
                        )
                    },
                    fileTaskLogs = it.fileTaskLogs?.map { fileTaskLog ->
                        FileDistributeLog(
                            ip = fileTaskLog.ip,
                            bkCloudId = fileTaskLog.bkCloudId,
                            bkHostId = fileTaskLog.bkHostId,
                            fileLogList = fileTaskLog.jobCloudFileLogList.map { jobCloudFileLog ->
                                FileLog(
                                    mode = jobCloudFileLog.mode,
                                    srcHost = parseHostInRes(jobCloudFileLog.srcHost),
                                    srcPath = jobCloudFileLog.srcPath,
                                    destHost = jobCloudFileLog.destHost?.let { destHost -> parseHostInRes(destHost) },
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

    private fun parseHostInRes(host: JobCloudHostInRes): HostInRes {
        return HostInRes(
            ip = host.ip,
            ipv6 = host.ipv6,
            bkCloudName = host.bkCloudName,
            bkCloudId = host.bkCloudId,
            bkHostId = host.bkHostId,
            bkAgentId = host.bkAgentId,
            alive = host.alive
        )
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
        ApigwJobCloudApi.setJobOperationName(::createAccount.name)

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
        ApigwJobCloudApi.setJobOperationName(::deleteAccount.name)

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
        ApigwJobCloudApi.setJobOperationName("queryJobInstanceStatus")
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
        ApigwJobCloudApi.setJobOperationName("getAccountList")
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
        ApigwJobCloudApi.setJobOperationName("getStepInstanceDetail")
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
                                    Account(
                                        id = jobCloudAccount.id,
                                        name = jobCloudAccount.name,
                                        alias = jobCloudAccount.alias
                                    )
                                },
                                server = parseVariableServer(jobCloudScriptStepInfo.server),
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
                                    server = jobCloudFileSource.server?.let { jobCloudVariableServer ->
                                        parseVariableServer(jobCloudVariableServer)
                                    },
                                    account = jobCloudFileSource.account.let { jobCloudAccount ->
                                        Account(
                                            id = jobCloudAccount.id,
                                            name = jobCloudAccount.name,
                                            alias = jobCloudAccount.alias
                                        )
                                    },
                                    fileSourceId = jobCloudFileSource.fileSourceId,
                                    fileSourceCode = jobCloudFileSource.fileSourceCode
                                )
                            },
                            fileDestination = jobCloudFileStepInfo.fileDestination.let { jobCloudFileDestination ->
                                FileDestination(
                                    path = jobCloudFileDestination.path,
                                    account = jobCloudFileDestination.account.let { jobCloudAccount ->
                                        Account(
                                            id = jobCloudAccount.id,
                                            name = jobCloudAccount.name,
                                            alias = jobCloudAccount.alias
                                        )
                                    },
                                    server = parseVariableServer(jobCloudFileDestination.server)
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

    private fun parseVariableServer(jobCloudVariableServer: JobCloudVariableServer): VariableServer {
        return VariableServer(
            variable = jobCloudVariableServer.variable,
            hostList = jobCloudVariableServer.jobCloudHostList?.map {
                HostIpv6(
                    ip = it.ip,
                    ipv6 = it.ipv6,
                    bkHostId = it.bkHostId,
                    bkCloudId = it.bkCloudId,
                    bkCloudName = it.bkCloudName,
                    bkAgentId = it.bkAgentId,
                    alive = it.alive
                )
            },
            topoNodeList = jobCloudVariableServer.jobCloudTopoNodeList?.map {
                TopoNode(
                    nodeType = it.nodeType,
                    id = it.id
                )
            },
            dynamicGroupList = jobCloudVariableServer.jobCloudDynamicGroupList?.map {
                DynamicGroup(id = it.id)
            }
        )
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
        ApigwJobCloudApi.setJobOperationName("getStepInstanceStatus")
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

    fun queryAgentStatusFromJob(
        queryAgentStatusFromJobReq: QueryAgentStatusFromJobReq
    ): JobResult<QueryAgentStatusFromJobResult> {
        ApigwJobCloudApi.setJobOperationName(::queryAgentStatusFromJob.name)
        val queryAgentStatusFromJobRequest = JobCloudQueryAgentStatusFromJobReq(
            hostIdList = queryAgentStatusFromJobReq.hostIdList,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        val agentQueryAgentStatusFromJobRes: JobCloudResult<JobCloudQueryAgentStatusFromJobResult> =
            apigwJobCloudApi.executePostRequest(
                queryAgentStatusFromJobRequest, JobCloudQueryAgentStatusFromJobResult::class.java
            )
        val queryAgentStatusFromJobRes: JobResult<QueryAgentStatusFromJobResult> = JobResult(
            code = agentQueryAgentStatusFromJobRes.code,
            result = agentQueryAgentStatusFromJobRes.result,
            jobRequestId = agentQueryAgentStatusFromJobRes.jobRequestId,
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
}