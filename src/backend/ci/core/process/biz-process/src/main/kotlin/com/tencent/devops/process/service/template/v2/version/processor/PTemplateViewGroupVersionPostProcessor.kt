package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PTemplateViewGroupVersionPostProcessor @Autowired constructor(
    private val pipelineGroupService: PipelineGroupService
) : PTemplateVersionCreatePostProcessor {

    override fun postProcessAfterVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (!context.versionAction.isCreateReleaseVersion()) return

            pipelineGroupService.updatePipelineLabel(
                userId = userId,
                projectId = pipelineTemplateSetting.projectId,
                pipelineId = pipelineTemplateSetting.pipelineId,
                labelIds = pipelineTemplateSetting.labels
            )
        }
    }
}
