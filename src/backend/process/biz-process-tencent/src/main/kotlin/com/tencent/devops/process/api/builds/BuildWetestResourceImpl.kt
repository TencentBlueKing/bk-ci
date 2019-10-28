package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.third.wetest.WetestResponse
import com.tencent.devops.process.service.wetest.WetestService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildWetestResourceImpl @Autowired constructor(
    private val wetestService: WetestService
) : BuildWetestResource {

    override fun save(response: WetestResponse, projectId: String, pipelineId: String, buildId: String): Result<Boolean> {
        wetestService.saveResponse(response, projectId, pipelineId, buildId)
        return Result(true)
    }
}