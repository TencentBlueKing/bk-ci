package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineBatchTaskListener {

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        logger.info(
            "pipeline batch task execute event received|projectId=${event.projectId}|" +
                "taskId=${event.taskId}|taskType=${event.taskType}"
        )
    }

    fun config(event: PipelineBatchTaskConfigEvent) {
        logger.info(
            "pipeline batch task config event received|projectId=${event.projectId}|" +
                "taskId=${event.taskId}|taskType=${event.taskType}"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBatchTaskListener::class.java)
    }
}
