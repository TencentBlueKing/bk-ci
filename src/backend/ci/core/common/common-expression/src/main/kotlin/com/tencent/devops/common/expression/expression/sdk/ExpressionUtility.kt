/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.NotSupportedException
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.ValueKind
import com.tencent.devops.common.expression.utils.FormatUtil

@Suppress("ComplexMethod", "ComplexCondition", "LongMethod", "ReturnCount")
object ExpressionUtility {

    fun convertToCanonicalValue(
        value: Any?
    ): Triple<ValueKind, Any?, Any?> {
        var raw: Any? = null
        val kind: ValueKind?

        if (value == null) {
            kind = ValueKind.Null
            return Triple(kind, raw, null)
        } else if (value is Boolean) {
            kind = ValueKind.Boolean
            return Triple(kind, raw, value)
        } else if (value is Double) {
            kind = ValueKind.Number
            return Triple(kind, raw, value)
        } else if (value is String) {
            kind = ValueKind.String
            return Triple(kind, raw, value)
        } else if (value is INull) {
            kind = ValueKind.Null
            raw = value
            return Triple(kind, raw, value)
        } else if (value is IBoolean) {
            kind = ValueKind.Boolean
            raw = value
            return Triple(kind, raw, value.getBoolean())
        } else if (value is INumber) {
            kind = ValueKind.Number
            raw = value
            return Triple(kind, raw, value.getNumber())
        } else if (value is IString) {
            kind = ValueKind.String
            raw = value
            return Triple(kind, raw, value.getString())
        } else if (value is IReadOnlyObject) {
            kind = ValueKind.Object
            return Triple(kind, raw, value)
        } else if (value is IReadOnlyArray<*>) {
            kind = ValueKind.Array
            return Triple(kind, raw, value)
        }

        if (value is Int || value is Byte || value is Long || value is Short) {
            kind = ValueKind.Number
            val v = when (value) {
                is Int -> value.toDouble()
                is Byte -> value.toDouble()
                is Long -> value.toDouble()
                is Short -> value.toDouble()
                else -> null
            }

            return Triple(kind, raw, v)
        } else if (value is Enum<*>) {
            val doubleValue = value.name.toDoubleOrNull()
            if (doubleValue != null) {
                kind = ValueKind.Number
                return Triple(kind, raw, value.name.toDoubleOrNull())
            }

            kind = ValueKind.String
            return Triple(kind, raw, value.name)
        }

        kind = ValueKind.Object
        return Triple(kind, raw, value)
    }

    fun formatValue(evaluationResult: EvaluationResult): String {
        return formatValue(evaluationResult.value, evaluationResult.kind)
    }

    fun formatValue(value: Any?, kind: ValueKind): String {
        when (kind) {
            ValueKind.Null -> return ExpressionConstants.NULL

            ValueKind.Boolean -> return if (value as Boolean) {
                ExpressionConstants.TRUE
            } else {
                ExpressionConstants.FALSE
            }

            ValueKind.Number -> return FormatUtil.doubleToString(value as Double)

            ValueKind.String -> return "'${stringEscape(value as String)}'"

            ValueKind.Array, ValueKind.Object ->
                return kind.toString()

            // Should never reach here.
            else ->
                throw NotSupportedException("Unable to convert to realized expression. Unexpected value kind: $kind")
        }
    }

    fun isLegalKeyword(str: String?): Boolean {
        if (str.isNullOrBlank()) {
            return false
        }

        val first = str[0]
        if ((first in 'a'..'z') ||
            (first in 'A'..'Z') ||
            first == '_'
        ) {
            str.subSequence(1, str.length).forEach { c ->
                if ((c in 'a'..'z') ||
                    (c in 'A'..'Z') ||
                    (c in '0'..'9') ||
                    c == '_' ||
                    c == '-'
                ) {
                    // OK
                } else {
                    return false
                }
            }
            return true
        } else {
            return false
        }
    }

    fun isPrimitive(kind: ValueKind): Boolean {
        return when (kind) {
            ValueKind.Null,
            ValueKind.Boolean,
            ValueKind.Number,
            ValueKind.String ->
                true
            else ->
                false
        }
    }

    /**
     * 此处的规则尝试遵循 Javascript 规则将字符串强制转换为数字
     * 用于比较。 即 Javascript 中的 Number() 函数。
     */
    fun parseNumber(s: String?): Double {
        // Trim
        val str = s?.trim() ?: ""

        // Empty
        if (str.isBlank()) {
            return 0.0
        }
        // Try parse
        else if (str.toDoubleOrNull() != null) {
            return str.toDoubleOrNull()!!
        }
        // Check for 0x[0-9a-fA-F]+
        else if (str[0] == '0' &&
            str.length > 2 &&
            str[1] == 'x' &&
            str.subSequence(2, str.length).all { x -> (x in '0'..'9') || (x in 'a'..'f') || (x in 'A'..'F') }
        ) {
            // Try parse
            if (str.toIntOrNull() != null) {
                return (str.toIntOrNull()!!).toDouble()
            }

            // Otherwise exceeds range
        }
        // Check for 0o[0-9]+
        else if (str[0] == '0' &&
            str.length > 2 &&
            str[1] == 'o' &&
            str.subSequence(2, str.length).all { x -> x in '0'..'7' }
        ) {
            // Try parse
            var integer: Int? = null
            try {
                integer = Integer.parseInt(str.substring(2), 8)
            }
            // Otherwise exceeds range
            catch (_: Exception) {
            }

            // Success
            if (integer != null) {
                return integer.toDouble()
            }
        }
        // Infinity
        else if (str == ExpressionConstants.INFINITY) {
            return Double.POSITIVE_INFINITY
        }
        // -Infinity
        else if (str == ExpressionConstants.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY
        }

        // Otherwise NaN
        return Double.NaN
    }

    fun stringEscape(value: String): String {
        return if (value.isBlank()) {
            ""
        } else {
            value.replace("'", "''")
        }
    }
}
