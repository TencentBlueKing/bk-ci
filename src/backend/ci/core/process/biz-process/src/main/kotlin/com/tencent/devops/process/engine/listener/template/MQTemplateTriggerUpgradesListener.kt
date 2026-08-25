package com.tencent.devops.process.engine.listener.template

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateTriggerUpgradesEvent
import com.tencent.devops.process.service.template.v2.PipelineTemplateMarketFacadeService
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线模板升级事件
 *
 * @version 1.0
 */
@Component
class MQTemplateTriggerUpgradesListener(
    private val pipelineTemplateMarketFacadeService: PipelineTemplateMarketFacadeService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineTemplateTriggerUpgradesEvent>(pipelineEventDispatcher) {
    override fun run(event: PipelineTemplateTriggerUpgradesEvent) {
        with(event) {
            val watcher = Watcher(id = "$traceId|triggerTemplateUpgrades#$templateId|$version|$projectId|$userId")
            try {
                logger.info(
                    "consumer template trigger upgrades event,{}|{}{}||{}", projectId, templateId, version, userId
                )
                watcher.start("triggerTemplateUpgrades")
                pipelineTemplateMarketFacadeService.releaseTemplateVersionAndTriggerUpgrades(
                    userId = userId,
                    projectId = projectId,
                    templateId = templateId,
                    version = version
                )
                watcher.stop()
            } finally {
                LogUtils.printCostTimeWE(watcher)
            }
        }
    }
}
