package com.tencent.devops.process.pojo.pipeline.task

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(
        value = PipelineBatchTaskFailedErrorCode::class,
        name = PipelineBatchTaskFailedErrorCode.classType
    ),
    JsonSubTypes.Type(
        value = PipelineBatchTaskFailedMsg::class,
        name = PipelineBatchTaskFailedMsg.classType
    ),
    JsonSubTypes.Type(
        value = PipelineBatchTaskDependencyFailed::class,
        name = PipelineBatchTaskDependencyFailed.classType
    )
)
@Schema(title = "流水线批量任务明细错误信息-基类")
interface PipelineBatchTaskErrorMessage {
    @JsonIgnore
    fun errorMessageText(): String
}
