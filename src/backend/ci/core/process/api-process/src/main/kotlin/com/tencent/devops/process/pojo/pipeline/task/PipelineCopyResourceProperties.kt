package com.tencent.devops.process.pojo.pipeline.task

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源属性")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@subType")
@JsonSubTypes(
    JsonSubTypes.Type(
        value = RepositoryCopyResourceProperties::class,
        name = RepositoryCopyResourceProperties.classType
    )
)
interface PipelineCopyResourceProperties
