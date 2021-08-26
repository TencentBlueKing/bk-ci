package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IServiceTurboPlanController
import com.tencent.devops.turbo.service.TurboPlanService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Suppress("MaxLineLength")
@RestController
class ServiceTurboPlanController @Autowired constructor(
    private val turboPlanService: TurboPlanService
) : IServiceTurboPlanController {

    override fun findTurboPlanIdByProjectIdAndPipelineInfo(projectId: String, pipelineId: String, pipelineElementId: String): Response<String?> {
        return Response.success(turboPlanService.findMigratedTurboPlanByPipelineInfo(projectId, pipelineId, pipelineElementId)?.taskId)
    }
}
