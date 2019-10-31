package com.tencent.devops.process.util

import java.util.regex.Pattern

object ParameterUtils {

    fun parseTemplate(variables: Map<String, Any>, template: String): String {
        if (template.isBlank()) {
            return template
        }
        val pattern = Pattern.compile("\\\$\\{([^}]+)}")
        val newValue = StringBuffer(template.length)
        val matcher = pattern.matcher(template)
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = variables[key]?.toString() ?: ""
            matcher.appendReplacement(newValue, value)
        }
        matcher.appendTail(newValue)
        return newValue.toString()
    }
}
