package com.tencent.devops.process.service.task

import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType

/**
 * 流水线批量任务处理器
 *
 * 不同任务类型实现该接口，提供创建、配置、删除等阶段的扩展能力。
 */
interface PipelineBatchTaskHandler {

    /**
     * 是否支持指定任务类型
     */
    fun support(taskType: PipelineBatchTaskType): Boolean

    /**
     * 创建任务时的初始任务状态
     */
    fun taskStatusWhenCreate(): PipelineBatchTaskStatus = PipelineBatchTaskStatus.DRAFT

    /**
     * 创建任务明细时的初始明细状态
     */
    fun detailStatusWhenCreate(): PipelineBatchTaskDetailStatus = PipelineBatchTaskDetailStatus.WAIT_COPY

    /**
     * 创建任务前的校验
     */
    fun validateWhenCreate(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ) = Unit

    /**
     * 创建任务后的业务处理
     */
    fun create(event: PipelineBatchTaskCreateEvent) = Unit

    /**
     * 配置任务前的校验
     */
    fun validateWhenConfig(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo,
        request: PipelineBatchTaskConfigRequest
    ) = Unit

    /**
     * 配置任务时的业务处理
     */
    fun config(event: PipelineBatchTaskConfigEvent) = Unit

    /**
     * 执行任务前的校验
     */
    fun validateWhenExecute(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo
    ) = Unit

    /**
     * 执行任务时的业务处理
     */
    fun execute(event: PipelineBatchTaskExecuteEvent) = Unit

    /**
     * 删除任务前的校验
     */
    fun validateWhenDelete(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo
    ) = Unit
}
