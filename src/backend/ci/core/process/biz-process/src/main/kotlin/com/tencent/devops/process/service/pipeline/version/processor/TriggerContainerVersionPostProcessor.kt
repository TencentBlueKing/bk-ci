package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.pipeline.task.PipelineTaskVersionProcessor
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerContainerVersionPostProcessor @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val taskVersionProcessors: List<PipelineTaskVersionProcessor>
) : PipelineVersionCreatePostProcessor {

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        if (pipelineResourceVersion.status != VersionStatus.RELEASED) {
            logger.warn(
                "pipeline version[${pipelineResourceVersion.status}] is not released, " +
                        "skip trigger container version post processor"
            )
            return
        }
        val triggerContainer = pipelineResourceVersion.model.getTriggerContainer()
        val variables = pipelineRepositoryService.getTriggerParams(triggerContainer)
        triggerContainer.elements.forEach { element ->
            taskVersionProcessors.firstOrNull { it.support(element) }
                    ?.postProcessAfterSave(
                        transactionContext = transactionContext,
                        context = context,
                        pipelineResourceVersion = pipelineResourceVersion,
                        pipelineSetting = pipelineSetting,
                        element = element,
                        variables = variables
                    )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerContainerVersionPostProcessor::class.java)
    }
}
