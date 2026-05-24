package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import com.tencent.devops.process.service.task.PipelineBatchTaskHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBatchTaskListener @Autowired constructor(
    private val handlers: List<PipelineBatchTaskHandler>
) {

    fun create(event: PipelineBatchTaskCreateEvent) {
        logger.info(
            "pipeline batch task create event received|projectId=${event.projectId}|" +
                "taskId=${event.taskId}|taskType=${event.taskType}"
        )
        getHandler(taskType = event.taskType).create(event)
    }

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        logger.info(
            "pipeline batch task execute event received|projectId=${event.projectId}|" +
                "taskId=${event.taskId}|taskType=${event.taskType}"
        )
        getHandler(taskType = event.taskType).execute(event)
    }

    fun config(event: PipelineBatchTaskConfigEvent) {
        logger.info(
            "pipeline batch task config event received|projectId=${event.projectId}|" +
                "taskId=${event.taskId}|taskType=${event.taskType}"
        )
        getHandler(taskType = event.taskType).config(event)
    }

    private fun getHandler(taskType: PipelineBatchTaskType): PipelineBatchTaskHandler {
        return handlers.singleOrNull { it.support(taskType) }
            ?: throw InvalidParamException("unsupported taskType: $taskType")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBatchTaskListener::class.java)
    }
}
