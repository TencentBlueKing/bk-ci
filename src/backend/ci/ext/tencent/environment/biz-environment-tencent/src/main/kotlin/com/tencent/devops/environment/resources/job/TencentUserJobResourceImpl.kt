package com.tencent.devops.environment.resources.job

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.job.TencentUserJobResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.RetryAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.TerminateAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.ObtainManualCommandResult
import com.tencent.devops.environment.pojo.job.agentres.OperateStepInstanceResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentInstallChannelResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskLogResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.RetryAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.TerminalAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.jobreq.OperateStepInstanceReq
import com.tencent.devops.environment.pojo.job.jobreq.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.JobResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.service.gseagent.GSEAgentService
import com.tencent.devops.environment.service.gseagent.InstallTaskService
import com.tencent.devops.environment.service.job.JobService
import com.tencent.devops.environment.service.job.PermissionManageService
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream
import javax.ws.rs.core.Response

@RestResource
class TencentUserJobResourceImpl @Autowired constructor(
    private val jobService: JobService,
    private val gseAgentService: GSEAgentService,
    private val installTaskService: InstallTaskService,
    private val permissionManageService: PermissionManageService
) : TencentUserJobResource {
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

    override fun getStepInstanceDetail(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult> {
        checkParamBlank(userId, projectId)
        checkJobInsBelongToProj(projectId, jobInstanceId)
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
        checkJobInsBelongToProj(projectId, jobInstanceId)
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

    override fun installAgent(
        userId: String,
        projectId: String,
        keyFile: InputStream?,
        installAgentReq: String
    ): AgentResult<InstallAgentResult> {
        checkParamBlank(userId, projectId)
        return gseAgentService.installAgent(userId, keyFile, installAgentReq)
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

    override fun queryAgentTaskLog(
        userId: String,
        projectId: String,
        jobId: Int,
        instanceId: String
    ): AgentResult<QueryAgentTaskLogResult> {
        checkParamBlank(userId, projectId)
        return installTaskService.queryAgentInstallTaskLog(jobId, instanceId)
    }

    override fun terminalAgentInstallTask(
        userId: String,
        projectId: String,
        jobId: Int,
        terminateAgentInstallTaskReq: TerminateAgentInstallTaskReq
    ): AgentResult<TerminalAgentInstallTaskResult> {
        checkParamBlank(userId, projectId)
        return gseAgentService.terminalAgentInstallTask(jobId, terminateAgentInstallTaskReq)
    }

    override fun retryAgentInstallTask(
        userId: String,
        projectId: String,
        jobId: Int,
        retryAgentInstallTaskReq: RetryAgentInstallTaskReq
    ): AgentResult<RetryAgentInstallTaskResult> {
        checkParamBlank(userId, projectId)
        return gseAgentService.retryAgentInstallTask(jobId, retryAgentInstallTaskReq)
    }

    override fun queryAgentInstallChannel(
        userId: String,
        projectId: String,
        withHidden: Boolean
    ): AgentResult<QueryAgentInstallChannelResult> {
        checkParamBlank(userId, projectId)
        return gseAgentService.queryAgentInstallChannel(withHidden)
    }

    override fun obtainManualInstallationCommand(
        userId: String,
        projectId: String,
        jobId: Int,
        hostId: Long
    ): AgentResult<ObtainManualCommandResult> {
        checkParamBlank(userId, projectId)
        return gseAgentService.obtainManualInstallationCommand(jobId, hostId)
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
                message = I18nUtil.getCodeLanMessage(EnvironmentMessageCode.ERROR_JOB_INSTANCE_NOT_BELONG_TO_PROJECT)
            )
        }
    }
}
