package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.service.PipelineBuildTemplateAcrossInfoService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildTemplateAcrossResourceImpl @Autowired constructor(
    private val templateAcrossInfoService: PipelineBuildTemplateAcrossInfoService
) : BuildTemplateAcrossResource {

    override fun getBuildAcrossTemplateInfo(
        projectId: String,
        pipelineId: String,
        templateId: String
    ): Result<List<BuildTemplateAcrossInfo>> {
        return Result(templateAcrossInfoService.getAcrossInfo(projectId, pipelineId, templateId))
    }
}
