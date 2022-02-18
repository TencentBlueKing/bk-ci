package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IBuildTurboPlanInstanceController
import com.tencent.devops.turbo.pojo.TurboPlanInstanceModel
import com.tencent.devops.turbo.service.TurboSummaryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class BuildTurboPlanInstanceController @Autowired constructor(
    private val turboSummaryService: TurboSummaryService
) : IBuildTurboPlanInstanceController {

    override fun upsertTurboPlanInstanceByPipelineInfo(
        turboPlanInstanceModel: TurboPlanInstanceModel,
        user: String
    ): Response<String> {
        return Response.success(turboSummaryService.updateSummaryPlanAndInstanceInfo(turboPlanInstanceModel, user))
    }
}
