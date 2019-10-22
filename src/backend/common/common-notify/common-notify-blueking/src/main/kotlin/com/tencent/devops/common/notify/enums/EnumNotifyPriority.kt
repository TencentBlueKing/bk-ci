package com.tencent.devops.common.notify.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class EnumNotifyPriority(private val property: String) {
    LOW("-1"),
    NORMAL("0"),
    HIGH("1");

    @JsonValue
    fun getValue(): String {
        return property
    }

    companion object {
        fun parse(value: String): EnumNotifyPriority {
            values().forEach { property ->
                if (property.getValue() == value) {
                    return property
                }
            }
            return HIGH
        }
    }
}