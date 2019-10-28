package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.BuildHistory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildHistoryBuildResourceImpl @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService
) : BuildHistoryBuildResource {
    override fun getSingleHistoryByBuildId(buildId: String): Result<BuildHistory?> {
        val buildIds = mutableSetOf<String>()
        buildIds.add(buildId)
        val history = pipelineRuntimeService.getBuildHistoryByIds(buildIds)[0]
        return Result(history)
    }
}
