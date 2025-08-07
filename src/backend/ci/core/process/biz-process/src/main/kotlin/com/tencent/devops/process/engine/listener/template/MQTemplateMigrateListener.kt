package com.tencent.devops.process.engine.listener.template

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateMigrateEvent
import com.tencent.devops.process.service.template.v2.PipelineTemplateMigrateService
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线模板迁移事件
 *
 * @version 1.0
 */
@Component
class MQTemplateMigrateListener(
    private val pipelineTemplateMigrateService: PipelineTemplateMigrateService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineTemplateMigrateEvent>(pipelineEventDispatcher) {
    override fun run(event: PipelineTemplateMigrateEvent) {
        with(event) {
            val watcher = Watcher(id = "$traceId|migrateTemplate#$templateId|$projectId|$userId")
            try {
                logger.info("consumer template migrate event,{}|{}|{}", projectId, templateId, userId)
                watcher.start("migrateTemplate")
                pipelineTemplateMigrateService.migrateTemplate(
                    projectId = projectId,
                    templateId = templateId
                )
                watcher.stop()
            } finally {
                LogUtils.printCostTimeWE(watcher)
            }
        }
    }
}
