package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import org.springframework.stereotype.Component

@Component
@Suppress("ALL")
interface PipelineBatchTaskListener {

    fun support(taskType: PipelineBatchTaskType): Boolean

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        if (!support(event.taskType)) {
            return
        }
        doExecute(event)
    }

    fun doExecute(event: PipelineBatchTaskExecuteEvent)

    fun validateWhenCreate(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ) = Unit

    fun validateWhenDelete(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo
    ) = Unit
}
