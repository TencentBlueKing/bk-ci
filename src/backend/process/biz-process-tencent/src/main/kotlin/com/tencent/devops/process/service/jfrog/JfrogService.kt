package com.tencent.devops.process.service.jfrog

import com.tencent.devops.process.engine.service.PipelineService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JfrogService @Autowired constructor(
    private val pipelineService: PipelineService
) {

    fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Map<String, String> {
        if (pipelineIds.isEmpty()) return mapOf()
        if (projectId.isBlank()) return mapOf()
        return pipelineService.getPipelineNameByIds(projectId, pipelineIds, true)
    }

    fun getBuildNoByBuildIds(projectId: String, pipelineId: String, buildIds: Set<String>): Map<String, String> {
        if (buildIds.isEmpty()) return mapOf()
        if (projectId.isBlank()) return mapOf()
        if (pipelineId.isBlank()) return mapOf()
        val buildNoByBuildIds = pipelineService.getBuildNoByBuildIds(projectId, pipelineId, buildIds)
        val result = mutableMapOf<String, String>()
        buildNoByBuildIds.forEach { t, u ->
            result[t] = u.toString()
        }
        return result
    }

    fun getBuildNoByByPair(buildIds: Set<String>): Map<String, String> {
        return pipelineService.getBuildNoByByPair(buildIds)
    }

    fun getArtifactoryCountFromHistory(startTime: Long, endTime: Long): Int
    {
        return pipelineService.getArtifacortyCountFormHistory(startTime, endTime)
    }
}
