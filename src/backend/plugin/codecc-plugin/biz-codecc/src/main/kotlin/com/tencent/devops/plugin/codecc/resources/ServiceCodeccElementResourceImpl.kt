package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.codecc.api.ServiceCodeccElementResource
import com.tencent.devops.plugin.codecc.pojo.CodeccElementData
import com.tencent.devops.plugin.codecc.service.CodeccElementService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCodeccElementResourceImpl @Autowired constructor(
    private val codeccElementService: CodeccElementService
) : ServiceCodeccElementResource {
    override fun get(projectId: String, pipelineId: String): Result<CodeccElementData> {
        return Result(codeccElementService.getCodeccElement(projectId, pipelineId))
    }
}