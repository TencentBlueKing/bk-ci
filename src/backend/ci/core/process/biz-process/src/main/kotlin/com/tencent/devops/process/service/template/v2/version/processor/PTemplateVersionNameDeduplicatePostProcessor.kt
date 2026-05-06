package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.jooq.DSLContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(0)
class PTemplateVersionNameDeduplicatePostProcessor(
    private val resourceService: PipelineTemplateResourceService
) : PTemplateVersionCreatePostProcessor {

    override fun postProcessInTransactionBeforeVersionCreate(
        transactionContext: DSLContext,
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        if (!context.versionAction.isCreateReleaseVersion()) return

        val targetName = context.customVersionName
            ?: pipelineTemplateResource.versionName
            ?: return

        resourceService.renameExistingReleasedVersionIfDuplicate(
            transactionContext = transactionContext,
            projectId = context.projectId,
            templateId = context.templateId,
            versionName = targetName
        )
    }
}
