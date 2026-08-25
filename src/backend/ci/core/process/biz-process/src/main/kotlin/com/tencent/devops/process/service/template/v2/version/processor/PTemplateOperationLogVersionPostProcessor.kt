package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建审计后置处理器
 */
@Service
class PTemplateOperationLogVersionPostProcessor(
    private val operationLogService: PipelineOperationLogService
) : PTemplateVersionCreatePostProcessor {
    override fun postProcessAfterVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            operationLogService.addOperationLog(
                userId = userId,
                projectId = projectId,
                pipelineId = templateId,
                version = pipelineTemplateResource.version.toInt(),
                operationLogType = operationLogType,
                params = operationLogParams,
                description = null
            )
        }
    }
}
