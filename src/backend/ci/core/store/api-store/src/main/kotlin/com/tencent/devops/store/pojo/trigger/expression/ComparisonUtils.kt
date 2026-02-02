package com.tencent.devops.store.pojo.trigger.expression

object ComparisonUtils {

    fun compare(eventValue: Any, inputValue: Any): Int {
        return when (eventValue) {
            is Number -> {
                compareNumbers(eventValue, inputValue as Number)
            }

            is String -> {
                eventValue.compareTo(inputValue as String)
            }

            is Boolean -> {
                eventValue.compareTo(inputValue as Boolean)
            }

            else -> throw IllegalArgumentException("only support number、boolean and string comparison")
        }
    }

    fun compareForEquality(eventValue: Any?, inputValue: Any?): Boolean {
        return when {
            eventValue == null && inputValue == null -> true
            eventValue == null || inputValue == null -> false
            else -> compare(eventValue, inputValue) == 0
        }
    }

    /**
     * 数字比较的内部实现
     */
    private fun compareNumbers(eventValue: Number, inputValue: Number): Int {
        return when {
            eventValue is Double || inputValue is Double ||
                    eventValue is Float || inputValue is Float -> {
                eventValue.toDouble().compareTo(inputValue.toDouble())
            }

            eventValue is Long || inputValue is Long -> {
                eventValue.toLong().compareTo(inputValue.toLong())
            }

            else -> {
                eventValue.toInt().compareTo(inputValue.toInt())
            }
        }
    }
}
