package com.tencent.devops.process.service.quality

import com.tencent.devops.process.pojo.template.TemplatePipeline

interface PipelineQualityInterfaceService {

    fun pipelineDetailChangeEvent(buildId: String)

    fun getPipelineTemplate(pipelineId: String): TemplatePipeline?
}