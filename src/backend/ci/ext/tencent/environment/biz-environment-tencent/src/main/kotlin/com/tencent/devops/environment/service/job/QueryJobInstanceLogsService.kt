package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.Host
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("QueryJobInstanceLogsService")
class QueryJobInstanceLogsService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""
    fun queryJobInstanceLogs(jobCloudQueryJobInstanceLogsReq: JobCloudQueryJobInstanceLogsReq): Result<QueryJobInstanceLogsResult> {
        val bkAppCode = appCode
        val bkAppSecret = appSecret
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"userId\": \"${jobCloudQueryJobInstanceLogsReq.bkUsername}\"}"
//        val queryJobInstanceLogsUrl = "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/batch_get_job_instance_ip_log/" // 正式 TODO：改为配置项
        val queryJobInstanceLogsUrl = "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/batch_get_job_instance_ip_log/" // 预发布 TODO：改为配置项
        jobCloudQueryJobInstanceLogsReq.bkAppCode = appCode!!
        jobCloudQueryJobInstanceLogsReq.bkAppSecret = appSecret!!
//        queryJobInstanceLogsJobCloudReq.bk_scope_type = bkScopeType!! // TODO：改为配置项
//        queryJobInstanceLogsJobCloudReq.bk_scope_id = bkScopeId!! // TODO：改为配置项

        val request = NetworkUtil.createPostRequest(
            url = queryJobInstanceLogsUrl,
            bkAuthorization = bkAuthorization,
            jobCloudReq = jobCloudQueryJobInstanceLogsReq::class.java
        )
        val jobCloudResp: JobCloudResp<QueryJobInstanceLogsResult> = NetworkUtil.executeHttpRequest("queryJobInstanceLogs", request)
        val queryJobInstanceLogsResult: QueryJobInstanceLogsResult
        if (1 == jobCloudResp.data?.logType) { // 1 - 脚本执行任务日志
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
        } else { // 2 - 文件分发任务日志
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

        return Result(
            status = jobCloudResp.code,
            message = jobCloudResp.message,
            data = queryJobInstanceLogsResult
        )
    }
}