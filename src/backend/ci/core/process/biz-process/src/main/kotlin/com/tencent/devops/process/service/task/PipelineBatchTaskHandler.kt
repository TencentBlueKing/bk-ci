package com.tencent.devops.process.service.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskAnalyzeEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import org.jooq.DSLContext

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
     * 创建任务明细时的初始明细状态
     */
    fun detailStatusWhenCreate(): PipelineBatchTaskDetailStatus

    /**
     * 创建任务前的校验
     */
    fun validateWhenCreate(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ) = Unit

    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineBatchTaskCreateRequest
    ) = Unit

    /**
     * 创建任务后的业务处理
     */
    fun handleCreateEvent(event: PipelineBatchTaskCreateEvent) = Unit

    /**
     * 处理任务分析事件
     */
    fun handleAnalyzeEvent(event: PipelineBatchTaskAnalyzeEvent) = Unit

    /**
     * 执行任务前的校验
     */
    fun validateWhenExecute(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo
    ) = Unit

    /**
     * 处理执行任务事件
     */
    fun handleExecuteEvent(event: PipelineBatchTaskExecuteEvent) = Unit

    /**
     * 删除任务前的校验
     */
    fun validateWhenDelete(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo
    ) = Unit
}
