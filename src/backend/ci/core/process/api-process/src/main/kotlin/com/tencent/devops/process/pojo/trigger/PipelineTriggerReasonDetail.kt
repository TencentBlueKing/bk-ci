package com.tencent.devops.process.pojo.trigger

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.annotations.ApiModel

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PipelineTriggerFailedMatch::class, name = PipelineTriggerFailedMatch.classType),
    JsonSubTypes.Type(value = PipelineTriggerFailedErrorCode::class, name = PipelineTriggerFailedErrorCode.classType),
    JsonSubTypes.Type(value = PipelineTriggerFailedMsg::class, name = PipelineTriggerFailedMsg.classType),
    JsonSubTypes.Type(value = PipelineTriggerFailedFix::class, name = PipelineTriggerFailedFix.classType),
)
@ApiModel("流水线触发事件原因详情-基类")
abstract class PipelineTriggerReasonDetail {
    @JsonIgnore
    abstract fun getReasonDetailList() : List<String>?
}
