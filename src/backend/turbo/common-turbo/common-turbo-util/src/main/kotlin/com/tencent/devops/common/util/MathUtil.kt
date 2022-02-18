package com.tencent.devops.common.util

object MathUtil {

    fun roundToTwoDigits(input: Double): String {
        return String.format("%.2f", input)
    }
}
