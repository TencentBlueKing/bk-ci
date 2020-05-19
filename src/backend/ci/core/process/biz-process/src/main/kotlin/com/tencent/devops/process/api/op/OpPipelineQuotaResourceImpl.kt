package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.PipelineQuotaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineQuotaResourceImpl @Autowired constructor(
    private val pipelineQuotaService: PipelineQuotaService
): OpPipelineQuotaResource {
    override fun updateQuota(userId: String, projectId: String, quota: Long): Result<Boolean> {
        pipelineQuotaService.setQuotaByProject(projectId, quota)
        return Result(true)
    }

    override fun updateUsedQuota(userId: String, projectId: String, usedQuota: Long): Result<Boolean> {
        pipelineQuotaService.setQuotaUsedByProject(projectId, usedQuota)
        return Result(true)
    }
}