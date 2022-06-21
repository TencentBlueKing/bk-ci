package com.tencent.devops.common.expression.resources

object ExpressionResources {
    fun exceededAllowedMemory(arg0: Any?): String {
        return "The maximum allowed memory size was exceeded while evaluating the following expression: $arg0"
    }

    fun invalidFormatArgIndex(arg0: Any?): String {
        return "The following format string references more arguments than were supplied: $arg0"
    }

    fun invalidFormatSpecifiers(arg0: Any?, arg1: Any?): String {
        return "The format specifiers '$arg0' are not valid for s of type '$arg1'"
    }

    fun invalidFormatString(arg0: Any?): String {
        return "The following format string is invalid: $arg0"
    }
}
