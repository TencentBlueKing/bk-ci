package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateTriggerUpgradesEvent
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建研发商店版本上架后置处理器
 */
@Service
class PTemplateMarketPublishedVersionPostProcessor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : PTemplateVersionCreatePostProcessor {

    override fun postProcessAfterVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (!versionAction.isCreateReleaseVersion()) {
                return
            }
            val templateInfo = pipelineTemplateInfoService.get(
                projectId = projectId,
                templateId = templateId
            )
            // 检查模板是否已上架到研发商店并设置发布策略为自动。
            val isTemplatePublishedToMarket = client.get(ServiceTemplateResource::class).getMarketTemplateStatus(
                templateCode = templateId
            ).data == TemplateStatusEnum.RELEASED
            if (!isTemplatePublishedToMarket || templateInfo.publishStrategy != UpgradeStrategyEnum.AUTO)
                return
            // 触发关联模板的自动升级
            pipelineEventDispatcher.dispatch(
                PipelineTemplateTriggerUpgradesEvent(
                    projectId = projectId,
                    source = "PIPELINE_TEMPLATE_TRIGGER_UPGRADES",
                    pipelineId = "",
                    userId = userId,
                    templateId = templateId,
                    version = pipelineTemplateResource.version
                )
            )
        }
    }
}
