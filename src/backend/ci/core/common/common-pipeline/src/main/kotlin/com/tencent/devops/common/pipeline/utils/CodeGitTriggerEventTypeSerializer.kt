package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType

class CodeGitTriggerEventTypeSerializer : JsonDeserializer<String>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): String {
        val originEventType = parser.text // 获取原始的字段值
        // merge_request_accept 事件本身就是merge_request事件，这里需要将其转换为merge_request事件
        return if (originEventType == CodeEventType.MERGE_REQUEST_ACCEPT.name) {
            CodeEventType.MERGE_REQUEST.name
        } else {
            originEventType
        }
    }
}