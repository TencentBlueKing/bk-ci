package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import org.springframework.stereotype.Service

@Service("ScriptExecuteService")
class ScriptExecuteService {
    fun executeScript(
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): ScriptExecuteResult {
        TODO()
    }
}