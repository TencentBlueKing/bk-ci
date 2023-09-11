package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.TaskTerminateResult

class TaskTerminateService {
    fun terminateTask(
        userId: String,
        projectId: String,
        taskTerminateReq: TaskTerminateReq
    ): TaskTerminateResult {
        TODO()
    }
}