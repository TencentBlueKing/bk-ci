package com.tencent.devops.common.util.enums

import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("CAST_NEVER_SUCCEEDS")
enum class ConfigParamType(private val valueType: String) {
    INPUT("String"),
    RADIO("String"),
    CHECKBOX("Array"),
    SELECT("String"),
    SWITCHER("Boolean"),
    DATEPICKER("LocalDate"),
    TIMEPICKER("LocalDateTime"),
    RADIOBUTTON("String");

    fun convertParamValue(paramValue: Any?): Any? {
        return try {
            if (null == paramValue) {
                null
            } else {
                when (this.valueType) {
                    "Boolean" -> paramValue as? Boolean
                    "Int" -> paramValue as? Int
                    "Double" -> paramValue as? Double
                    "Array" -> paramValue as? Set<*>
                    "String" -> paramValue as? String
                    "LocalDate" -> LocalDate.parse(paramValue.toString())
                    "LocalDateTime" -> LocalDateTime.parse(paramValue.toString())
                    else -> paramValue
                }
            }
        } catch (t: Throwable) {
            paramValue
        }
    }
}
