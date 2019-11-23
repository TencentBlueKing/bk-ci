package com.tencent.devops.common.ci.task

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.TASK_TYPE
import com.tencent.devops.common.pipeline.pojo.element.Element

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = TASK_TYPE)
@JsonSubTypes(
    JsonSubTypes.Type(value = BashTask::class, name = BashTask.taskType + BashTask.taskVersion),
    JsonSubTypes.Type(value = CodeCCScanTask::class, name = CodeCCScanTask.taskType + CodeCCScanTask.taskVersion),
    JsonSubTypes.Type(value = DockerRunDevCloudTask::class, name = DockerRunDevCloudTask.taskType + DockerRunDevCloudTask.taskVersion),
    JsonSubTypes.Type(value = DockerBuildAndPushImageTask::class, name = DockerBuildAndPushImageTask.taskType + DockerBuildAndPushImageTask.taskVersion),
    JsonSubTypes.Type(value = MarketBuildTask::class, name = MarketBuildTask.taskType + MarketBuildTask.taskVersion),
    JsonSubTypes.Type(value = CodeCCScanInContainerTask::class, name = CodeCCScanInContainerTask.taskType + CodeCCScanInContainerTask.taskVersion)
)

abstract class AbstractTask(
    open val displayName: String?,
    open val inputs: AbstractInput?,
    open val condition: String?
) {
    abstract fun getTaskType(): String
    abstract fun getTaskVersion(): String

    abstract fun covertToElement(config: CiBuildConfig): Element
}

abstract class AbstractInput