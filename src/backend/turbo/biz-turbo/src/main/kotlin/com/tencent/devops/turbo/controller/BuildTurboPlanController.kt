package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IBuildTurboPlanController
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Suppress("MaxLineLength")
@RestController
class BuildTurboPlanController @Autowired constructor(
    private val turboPlanService: TurboPlanService
) : IBuildTurboPlanController {

    override fun findTurboPlanIdByProjectIdAndPipelineInfo(projectId: String, pipelineId: String, pipelineElementId: String): Response<String?> {
        return Response.success(turboPlanService.findMigratedTurboPlanByPipelineInfo(projectId, pipelineId, pipelineElementId)?.taskId)
    }

    override fun findTurboPlanDetailById(
        turboPlanId: String
    ): Response<TurboPlanDetailVO> {
        return Response.success(turboPlanService.getTurboPlanDetailByPlanId(turboPlanId))
    }
}
