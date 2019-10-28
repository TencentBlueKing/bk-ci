package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildCodeccResource
import com.tencent.devops.plugin.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService
) : BuildCodeccResource {
    override fun queryCodeccTaskDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        return codeccService.queryCodeccTaskDetailUrl(projectId, pipelineId, buildId)
    }

    override fun saveCodeccTask(projectId: String, pipelineId: String, buildId: String): Result<Int> {
        return Result(codeccService.saveCodeccTask(projectId, pipelineId, buildId))
    }
}
