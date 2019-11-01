package com.tencent.devops.prebuild.pojo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.prebuild.service.PreBuildConfig

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "task")
@JsonSubTypes(
    JsonSubTypes.Type(value = LinuxCodeCCTask::class, name = LinuxCodeCCTask.classType),
    JsonSubTypes.Type(value = LinuxScriptTask::class, name = LinuxScriptTask.classType),
    JsonSubTypes.Type(value = CodeCCScanTask::class, name = CodeCCScanTask.classType)
)
abstract class AbstractTask(
    open val displayName: String,
    open val input: AbstractInput
) {
    abstract fun getClassType(): String
    abstract fun covertToElement(config: PreBuildConfig): Element
}

abstract class AbstractInput