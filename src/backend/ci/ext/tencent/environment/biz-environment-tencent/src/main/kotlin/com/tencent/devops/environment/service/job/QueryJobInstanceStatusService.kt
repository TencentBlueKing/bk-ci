package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import org.springframework.stereotype.Service

@Service("QueryJobInstanceStatusService")
class QueryJobInstanceStatusService {
    fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long
    ): QueryJobInstanceStatusResult {
        TODO()
    }
}