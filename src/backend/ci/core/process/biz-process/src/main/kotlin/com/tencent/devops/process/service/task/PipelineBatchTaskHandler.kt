package com.tencent.devops.process.service.task

import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType

interface PipelineBatchTaskHandler {

    fun support(taskType: PipelineBatchTaskType): Boolean

    fun validateWhenCreate(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ) = Unit

    fun validateWhenConfig(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo,
        request: PipelineBatchTaskConfigRequest
    ) = Unit

    fun config(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo,
        request: PipelineBatchTaskConfigRequest
    ) = Unit

    fun validateWhenDelete(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo
    ) = Unit
}
