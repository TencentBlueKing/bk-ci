package com.tencent.devops.common.notify.blueking.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class EnumEmailType(private val type: Int) {
    OUTER_MAIL(0),
    INNER_MAIL(1);

    @JsonValue
    fun getValue(): Int {
        return type
    }

    companion object {
        fun parse(value: Int?): EnumEmailType {
            values().forEach { type ->
                if (type.getValue() == value) {
                    return type
                }
            }
            return OUTER_MAIL
        }
    }
}