package com.tencent.devops.process.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.statistic.PipelineAndTemplateStatistic
import com.tencent.devops.process.service.PipelineTemplateStatisticService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStatisticResourceImpl @Autowired constructor(
    private val pipelineTemplateStatisticService: PipelineTemplateStatisticService
) : ServiceStatisticResource {
    override fun getPipelineAndTemplateStatistic(
        userId: String,
        organizationType: String,
        organizationId: Int,
        deptName: String?,
        centerName: String?
    ): Result<PipelineAndTemplateStatistic> {
        return Result(
            pipelineTemplateStatisticService.getPipelineAndTemplateStatistic(
                userId = userId,
                organizationType = organizationType,
                organizationId = organizationId,
                deptName = deptName,
                centerName = centerName,
                interfaceName = "/service/v2/statistics/pipelinesAndTemplates"
            )
        )
    }
}