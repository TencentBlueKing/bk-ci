package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.v2.ApigwStatisticResource
import com.tencent.devops.process.api.v2.ServiceStatisticResource
import com.tencent.devops.process.pojo.statistic.PipelineAndTemplateStatistic
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStatisticResourceImpl @Autowired constructor(
    private val client: Client
) : ApigwStatisticResource {
    override fun getPipelineAndTemplateStatistic(
        userId: String,
        organizationType: String,
        organizationId: Int,
        deptName: String?,
        centerName: String?
    ): Result<PipelineAndTemplateStatistic> {
        return client.get(ServiceStatisticResource::class).getPipelineAndTemplateStatistic(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName,
            centerName = centerName
        )
    }
}