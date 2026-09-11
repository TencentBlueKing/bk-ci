package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.op.OpAiOverviewResource
import com.tencent.devops.ai.pojo.AiOverviewVO
import com.tencent.devops.ai.service.AiOverviewService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAiOverviewResourceImpl @Autowired constructor(
    private val overviewService: AiOverviewService
) : OpAiOverviewResource {
    override fun get(): Result<AiOverviewVO> {
        return Result(overviewService.getOverview())
    }
}
