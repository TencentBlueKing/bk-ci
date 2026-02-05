package com.tencent.devops.process.engine.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.service.ServiceModelHandleResource
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.pipeline.ModelPublicVarHandleContext

@RestResource
class ServiceModelHandleResourceImpl(private val modelHandleService: ModelHandleService) : ServiceModelHandleResource {
    override fun handlePipelineModelParams(
        projectId: String,
        modelPublicVarHandleContext: ModelPublicVarHandleContext
    ): Result<List<BuildFormProperty>> {
        return Result(modelHandleService.handleModelParams(projectId, modelPublicVarHandleContext))
    }
}
