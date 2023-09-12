package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.TaskTerminateResult
import org.springframework.stereotype.Service

@Service("TaskTerminateService")
class TaskTerminateService {
    fun terminateTask(
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): TaskTerminateResult {
        TODO()
    }
}