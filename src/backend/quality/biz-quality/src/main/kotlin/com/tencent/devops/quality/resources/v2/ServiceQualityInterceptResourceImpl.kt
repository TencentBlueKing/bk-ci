package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.quality.api.v2.pojo.QualityRuleIntercept
import com.tencent.devops.quality.service.v2.QualityHistoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityInterceptResourceImpl @Autowired constructor(
    private val historyService: QualityHistoryService
) : ServiceQualityInterceptResource {
    override fun listHistory(projectId: String, pipelineId: String, buildId: String): Result<List<QualityRuleIntercept>> {
        return Result(historyService.serviceListByBuildId(projectId, pipelineId, buildId))
    }
}