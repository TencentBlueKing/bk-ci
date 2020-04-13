package com.tencent.devops.process.service.quality

import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.template.TemplatePipeline
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CorePipelineQualityService @Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val templateService: TemplateService
) : PipelineQualityInterfaceService {
    override fun pipelineDetailChangeEvent(buildId: String) {
        pipelineBuildDetailService.pipelineDetailChangeEvent(buildId)
    }

    override fun getPipelineTemplate(pipelineId: String): TemplatePipeline? {
        return templateService.getTemplate(pipelineId)
    }
}