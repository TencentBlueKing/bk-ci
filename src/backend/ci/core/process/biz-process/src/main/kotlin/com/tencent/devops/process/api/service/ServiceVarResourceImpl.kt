package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.BuildVariableService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceVarResourceImpl @Autowired constructor(
    private val buildVariableService: BuildVariableService
) : ServiceVarResource {
    override fun getBuildVar(buildId: String, projectId: String, pipelineId: String): Result<Map<String, String>> {
        return Result(buildVariableService.getAllVariable(buildId))
    }
}