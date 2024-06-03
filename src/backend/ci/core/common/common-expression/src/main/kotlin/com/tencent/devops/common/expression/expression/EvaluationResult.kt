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

package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.ExpressionUtility
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyArray
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyObject
import com.tencent.devops.common.expression.utils.FormatUtil

@Suppress("EmptyIfBlock", "TooManyFunctions", "ReturnCount", "ComplexMethod")
class EvaluationResult(
    val context: EvaluationContext?,
    private val level: Int,
    val value: Any?,
    val kind: ValueKind,
    val raw: Any?,
    private val omitTracing: Boolean = false
) {
    init {
        if (!omitTracing) {
            traceValue(context)
        }
    }

    val equalsFalse: Boolean
        get() {
            when (kind) {
                ValueKind.Null -> return true
                ValueKind.Boolean -> {
                    val boolean = value as Boolean
                    return !boolean
                }

                ValueKind.Number -> {
                    val number = value as Double
                    return number == 0.0 || number.isNaN()
                }

                ValueKind.String -> {
                    val str = value as String
                    // 针对string是true和false做特殊处理
                    if (str == "false" || str == "true") {
                        return !str.toBoolean()
                    }

                    return str.isBlank()
                }

                else -> return false
            }
        }

    val isPrimitive: Boolean get() = ExpressionUtility.isPrimitive(kind)

    val equalsTrue: Boolean get() = !equalsFalse

    /**
     * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
     * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
     */
    fun abstractEqual(right: EvaluationResult): Boolean {
        return abstractEqual(value, right.value)
    }

    /**
     * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
     * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
     */
    fun abstractGreaterThan(right: EvaluationResult): Boolean {
        return abstractGreaterThan(value, right.value)
    }

    /**
     * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
     * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
     */
    fun abstractGreaterThanOrEqual(right: EvaluationResult): Boolean {
        return abstractEqual(value, right.value) || abstractGreaterThan(value, right.value)
    }

    /**
     * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
     * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
     */
    fun abstractLessThan(right: EvaluationResult): Boolean {
        return abstractLessThan(value, right.value)
    }

    /**
     * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
     * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
     */
    fun abstractLessThanOrEqual(right: EvaluationResult): Boolean {
        return abstractEqual(value, right.value) || abstractLessThan(value, right.value)
    }

    /**
     * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
     * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
     */
    fun abstractNotEqual(right: EvaluationResult): Boolean {
        return !abstractEqual(value, right.value)
    }

    fun convertToNumber(): Double {
        return convertToNumber(value)
    }

    fun convertToString(): String {
        return when (kind) {
            ValueKind.Null -> ""
            ValueKind.Boolean ->
                if (value as Boolean) {
                    ExpressionConstants.TRUE
                } else {
                    ExpressionConstants.FALSE
                }

            ValueKind.Number -> FormatUtil.doubleToString(value as Double)
            ValueKind.String -> value as String
            else -> kind.toString()
        }
    }

    fun tryGetCollectionInterface(): Pair<Any?, Boolean> {
        if ((kind == ValueKind.Object || kind == ValueKind.Array)) {
            val obj = value
            if (obj is IReadOnlyObject) {
                return Pair(obj, true)
            } else if (obj is IReadOnlyArray<*>) {
                return Pair(obj, true)
            }
        }

        return Pair(null, false)
    }

    private fun traceValue(context: EvaluationContext?) {
        if (!omitTracing) {
            traceValue(context, value, kind)
        }
    }

    private fun traceValue(
        context: EvaluationContext?,
        value: Any?,
        kind: ValueKind
    ) {
        if (!omitTracing) {
            traceVerbose(context, "=> ".plus(ExpressionUtility.formatValue(value, kind)))
        }
    }

    private fun traceVerbose(
        context: EvaluationContext?,
        message: String
    ) {
        if (!omitTracing) {
            context?.trace?.verbose("".padStart(level * 2, '.') + message)
        }
    }

    companion object {
        /**
         * Useful for working with values that are not the direct evaluation result of a parameter.
         * This allows ExpressionNode authors to leverage the coercion and comparision functions
         * for any values.
         *
         * Also note, the value will be canonicalized (for example numeric types converted to double) and any
         * matching interfaces applied.
         */
        fun createIntermediateResult(
            context: EvaluationContext,
            obj: Any?
        ): EvaluationResult {
            val (kind, raw, value) = ExpressionUtility.convertToCanonicalValue(obj)
            return EvaluationResult(context, 0, value, kind, raw, true)
        }

        /**
         * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
         * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
         */
        private fun abstractEqual(
            leftValue: Any?,
            rightValue: Any?
        ): Boolean {
            var canonicalLeftValue = leftValue
            var canonicalRightValue = rightValue
            val (v, k) = coerceTypes(canonicalLeftValue, canonicalRightValue)
            canonicalLeftValue = v.first
            canonicalRightValue = v.second
            val leftKind = k.first
            val rightKind = k.second

            // Same kind
            if (leftKind == rightKind) {
                when (leftKind) {
                    // Null, Null
                    ValueKind.Null -> return true

                    // Number, Number
                    ValueKind.Number -> {
                        val leftDouble = canonicalLeftValue as Double
                        val rightDouble = canonicalRightValue as Double
                        if (leftDouble.isNaN() || rightDouble.isNaN()) {
                            return false
                        }

                        return leftDouble == rightDouble
                    }

                    // String, String
                    ValueKind.String -> {
                        val leftString = canonicalLeftValue as String
                        val rightString = canonicalRightValue as String
                        return leftString == rightString
                    }

                    // Boolean, Boolean
                    ValueKind.Boolean -> {
                        val leftBoolean = canonicalLeftValue as Boolean
                        val rightBoolean = canonicalRightValue as Boolean
                        return leftBoolean == rightBoolean
                    }

                    // Object, Object
                    ValueKind.Object, ValueKind.Array ->
                        return canonicalLeftValue == canonicalRightValue
                }
            }

            return false
        }

        /**
         * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
         * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
         */
        private fun abstractGreaterThan(
            leftValue: Any?,
            rightValue: Any?
        ): Boolean {

            val (v, k) = coerceTypes(leftValue, rightValue)
            val canonicalLeftValue = v.first
            val canonicalRightValue = v.second
            val leftKind = k.first
            val rightKind = k.second

            // Same kind
            if (leftKind == rightKind) {
                when (leftKind) {
                    // Number, Number
                    ValueKind.Number -> {
                        val leftDouble = canonicalLeftValue as Double
                        val rightDouble = canonicalRightValue as Double
                        if (leftDouble.isNaN() || rightDouble.isNaN()) {
                            return false
                        }

                        return leftDouble > rightDouble
                    }

                    // String, String
                    ValueKind.String -> {
                        val leftString = canonicalLeftValue as String
                        val rightString = canonicalRightValue as String
                        return leftString > rightString
                    }

                    // Boolean, Boolean
                    ValueKind.Boolean -> {
                        val leftBoolean = canonicalLeftValue as Boolean
                        val rightBoolean = canonicalRightValue as Boolean
                        return leftBoolean && !rightBoolean
                    }

                    else -> {}
                }
            }

            return false
        }

        /**
         * Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
         * Except string comparison is OrdinalIgnoreCase, and objects are not coerced to primitives.
         */
        private fun abstractLessThan(
            leftValue: Any?,
            rightValue: Any?
        ): Boolean {

            val (v, k) = coerceTypes(leftValue, rightValue)
            val canonicalLeftValue = v.first
            val canonicalRightValue = v.second
            val leftKind = k.first
            val rightKind = k.second

            // Same kind
            if (leftKind == rightKind) {
                when (leftKind) {
                    // Number, Number
                    ValueKind.Number -> {
                        val leftDouble = canonicalLeftValue as Double
                        val rightDouble = canonicalRightValue as Double
                        if (leftDouble.isNaN() || rightDouble.isNaN()) {
                            return false
                        }

                        return leftDouble < rightDouble
                    }

                    // String, String
                    ValueKind.String -> {
                        val leftString = canonicalLeftValue as String
                        val rightString = canonicalRightValue as String
                        return leftString < rightString
                    }

                    // Boolean, Boolean
                    ValueKind.Boolean -> {
                        val leftBoolean = canonicalLeftValue as Boolean
                        val rightBoolean = canonicalRightValue as Boolean
                        return !leftBoolean && rightBoolean
                    }

                    else -> {}
                }
            }

            return false
        }

        // / Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
        // / Except objects are not coerced to primitives.
        private fun coerceTypes(
            leftValue: Any?,
            rightValue: Any?
        ): Pair<Pair<Any?, Any?>, Pair<ValueKind, ValueKind>> {
            var canonicalLeftValue = leftValue
            var canonicalRightValue = rightValue

            var leftKind = getKind(canonicalLeftValue)
            var rightKind = getKind(canonicalRightValue)

            // Same kind
            if (leftKind == rightKind) {
                //
            }
            // Number, String
            else if (leftKind == ValueKind.Number && rightKind == ValueKind.String) {
                canonicalRightValue = convertToNumber(canonicalRightValue)
                rightKind = ValueKind.Number
            }
            // String, Number
            else if (leftKind == ValueKind.String && rightKind == ValueKind.Number) {
                canonicalLeftValue = convertToNumber(canonicalLeftValue)
                leftKind = ValueKind.Number
            }
            // Boolean, Any
            else if (leftKind == ValueKind.Boolean) {
                // 针对string是true和false做特殊处理
                if (rightKind == ValueKind.String &&
                    (canonicalRightValue == "true" || canonicalRightValue == "false")
                ) {
                    canonicalRightValue = canonicalRightValue == "true"
                }
                canonicalLeftValue = convertToNumber(canonicalLeftValue)
                val (v, k) = coerceTypes(canonicalLeftValue, canonicalRightValue)
                canonicalLeftValue = v.first
                canonicalRightValue = v.second
                leftKind = k.first
                rightKind = k.second
            }
            // Any, Boolean
            else if (rightKind == ValueKind.Boolean) {
                // 针对string是true和false做特殊处理
                if (leftKind == ValueKind.String &&
                    (canonicalLeftValue == "true" || canonicalLeftValue == "false")
                ) {
                    canonicalLeftValue = canonicalLeftValue == "true"
                }
                canonicalRightValue = convertToNumber(canonicalRightValue)
                val (v, k) = coerceTypes(canonicalLeftValue, canonicalRightValue)
                canonicalLeftValue = v.first
                canonicalRightValue = v.second
                leftKind = k.first
                rightKind = k.second
            }
            // Null, Any
            else if (leftKind == ValueKind.Null) {
                canonicalLeftValue = convertToNumber(canonicalLeftValue)
                val (v, k) = coerceTypes(canonicalLeftValue, canonicalRightValue)
                canonicalLeftValue = v.first
                canonicalRightValue = v.second
                leftKind = k.first
                rightKind = k.second
            }
            // Any, Null
            else if (rightKind == ValueKind.Null) {
                canonicalRightValue = convertToNumber(canonicalRightValue)
                val (v, k) = coerceTypes(canonicalLeftValue, canonicalRightValue)
                canonicalLeftValue = v.first
                canonicalRightValue = v.second
                leftKind = k.first
                rightKind = k.second
            }

            return Pair(Pair(canonicalLeftValue, canonicalRightValue), Pair(leftKind, rightKind))
        }

        private fun convertToNumber(canonicalValue: Any?): Double {
            when (getKind(canonicalValue)) {
                ValueKind.Null -> return 0.0
                ValueKind.Boolean ->
                    return if (canonicalValue as Boolean) {
                        1.0
                    } else {
                        0.0
                    }

                ValueKind.Number ->
                    return canonicalValue as Double

                ValueKind.String ->
                    return ExpressionUtility.parseNumber(canonicalValue as String)

                else -> {}
            }

            return Double.NaN
        }

        private fun getKind(canonicalValue: Any?): ValueKind {
            when (canonicalValue) {
                null -> {
                    return ValueKind.Null
                }

                is Boolean -> {
                    return ValueKind.Boolean
                }

                is Double -> {
                    return ValueKind.Number
                }

                is String -> {
                    return ValueKind.String
                }

                is IReadOnlyObject -> {
                    return ValueKind.Object
                }

                is IReadOnlyArray<*> -> {
                    return ValueKind.Array
                }

                else -> return ValueKind.Object
            }
        }
    }
}
