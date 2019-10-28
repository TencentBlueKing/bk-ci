package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.jfrog.JfrogService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceJfrogResourceImpl @Autowired constructor(
    private val jfrogService: JfrogService
) : ServiceJfrogResource {

    override fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Result<Map<String, String>> {
        return Result(jfrogService.getPipelineNameByIds(projectId, pipelineIds))
    }

    override fun getBuildNoByBuildIds(projectId: String, pipelineId: String, buildIds: Set<String>): Result<Map<String, String>> {
        return Result(jfrogService.getBuildNoByBuildIds(projectId, pipelineId, buildIds))
    }

    override fun getBuildNoByBuildIds(buildIds: Set<String>): Result<Map<String, String>> {
        return Result(jfrogService.getBuildNoByByPair(buildIds))
    }

    override fun getArtifactoryCountFromHistory(startTime: Long, endTime: Long): Result<Int> {
        return Result(jfrogService.getArtifactoryCountFromHistory(startTime, endTime))
    }
}