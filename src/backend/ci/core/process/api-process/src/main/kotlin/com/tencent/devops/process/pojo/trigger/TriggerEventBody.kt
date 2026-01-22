package com.tencent.devops.process.pojo.trigger

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 触发事件body
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ScmWebhookEventBody::class, name = ScmWebhookEventBody.classType)
)
interface TriggerEventBody
