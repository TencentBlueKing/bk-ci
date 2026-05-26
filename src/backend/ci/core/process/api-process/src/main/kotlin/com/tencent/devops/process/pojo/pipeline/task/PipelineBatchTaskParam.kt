package com.tencent.devops.process.pojo.pipeline.task

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务参数")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PipelineBatchCopyTaskParam::class, name = PipelineBatchCopyTaskParam.classType)
)
interface PipelineBatchTaskParam
