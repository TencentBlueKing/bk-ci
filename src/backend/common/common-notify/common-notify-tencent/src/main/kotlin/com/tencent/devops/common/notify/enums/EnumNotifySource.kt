package com.tencent.devops.common.notify.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class EnumNotifySource(
    private val value: Int
) {
    BUSINESS_LOGIC(0),
    OPERATION(1);

    @JsonValue
    fun getValue(): Int {
        return value
    }

    companion object {
        fun parse(value: Int?): EnumNotifySource? {
            values().forEach { source ->
                if (source.getValue() == value) {
                    return source
                }
            }
            return null
        }
        fun parseName(sourceString: String): EnumNotifySource {
            values().forEach { source ->
                if (source.name == sourceString) {
                    return source
                }
            }
            return BUSINESS_LOGIC
        }
    }
}
