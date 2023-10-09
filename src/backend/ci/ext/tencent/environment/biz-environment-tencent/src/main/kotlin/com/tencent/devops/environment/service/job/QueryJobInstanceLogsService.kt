package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.Host
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryJobInstanceLogsService")
class QueryJobInstanceLogsService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun queryJobInstanceLogs(
        jobCloudQueryJobInstanceLogsReq: JobCloudQueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "queryJobInstanceLogs",
                operationEnv = "stag",
                bkUsername = jobCloudQueryJobInstanceLogsReq.bkUsername
            )
        jobCloudQueryJobInstanceLogsReq.bkAppCode = jobCloudAuthenticationReq.bkAppCode
        jobCloudQueryJobInstanceLogsReq.bkAppSecret = jobCloudAuthenticationReq.bkAppSecret
        jobCloudQueryJobInstanceLogsReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudQueryJobInstanceLogsReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<QueryJobInstanceLogsResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                operateName = "queryJobInstanceLogs",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudQueryJobInstanceLogsReq.toMap()
            )

        val queryJobInstanceLogsResult: QueryJobInstanceLogsResult
        when (jobCloudResp.data?.logType) {
            1 -> { // 1 - 脚本执行任务日志
                queryJobInstanceLogsResult = QueryJobInstanceLogsResult(
                    host = Host(
                        bkCloudId = jobCloudResp.data?.host?.bkCloudId,
                        ip = jobCloudResp.data?.host?.ip,
                        bkHostId = jobCloudResp.data?.host?.bkHostId
                    ),
                    logType = jobCloudResp.data?.logType!!,
                    scriptTaskLogs = jobCloudResp.data?.scriptTaskLogs,
                    fileTaskLogs = null
                )
            }
            2 -> { // 2 - 文件分发任务日志
                queryJobInstanceLogsResult = QueryJobInstanceLogsResult(
                    host = Host(
                        bkCloudId = jobCloudResp.data?.host?.bkCloudId,
                        ip = jobCloudResp.data?.host?.ip,
                        bkHostId = jobCloudResp.data?.host?.bkHostId
                    ),
                    logType = jobCloudResp.data?.logType!!,
                    scriptTaskLogs = null,
                    fileTaskLogs = jobCloudResp.data?.fileTaskLogs
                )
            }
            else -> {
                queryJobInstanceLogsResult = QueryJobInstanceLogsResult(
                    host = Host(
                        bkCloudId = null,
                        ip = null,
                        bkHostId = null
                    ),
                    logType = jobCloudResp.data?.logType!!,
                    scriptTaskLogs = null,
                    fileTaskLogs = null
                )
            }
        }

        return JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = queryJobInstanceLogsResult
        )
    }
}