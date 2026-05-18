package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import org.springframework.stereotype.Component

@Component
@Suppress("ALL")
interface PipelineBatchTaskListener {

    fun taskType(): PipelineBatchTaskType

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        if (event.taskType != taskType()) {
            return
        }
        doExecute(event)
    }

    fun doExecute(event: PipelineBatchTaskExecuteEvent)
}
