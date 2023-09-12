package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import org.springframework.stereotype.Service

@Service("QueryJobInstanceLogsService")
class QueryJobInstanceLogsService {
    fun queryJobInstanceLogs(
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): QueryJobInstanceLogsResult {
        TODO()
    }
}