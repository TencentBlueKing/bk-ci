package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.ExpressionUtility
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyArray
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyObject

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

    val isFalsy: Boolean
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
                    return str.isBlank()
                }
                else -> return false
            }
        }

    val isPrimitive: Boolean get() = ExpressionUtility.isPrimitive(kind)

    val isTruthy: Boolean get() = !isFalsy

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
            ValueKind.Number -> (value as Double).toString()
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
            context?.tarce?.verbose("".padStart(level * 2, '.') + message)
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
            rightValue: Any?,
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
            rightValue: Any?,
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
                }
            }

            return false
        }

        // / Similar to the Javascript abstract equality comparison algorithm http://www.ecma-international.org/ecma-262/5.1/#sec-11.9.3.
        // / Except objects are not coerced to primitives.
        private fun coerceTypes(
            leftValue: Any?,
            rightValue: Any?,
        ): Pair<Pair<Any?, Any?>, Pair<ValueKind, ValueKind>> {
            var canonicalLeftValue = leftValue
            var canonicalRightValue = rightValue

            var leftKind = getKind(canonicalLeftValue)
            var rightKind = getKind(canonicalRightValue)

            // Same kind
            if (leftKind == rightKind) {
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
            // Boolean|Null, Any
            else if (leftKind == ValueKind.Boolean || leftKind == ValueKind.Null) {
                canonicalLeftValue = convertToNumber(canonicalLeftValue)
                val (v, k) = coerceTypes(canonicalLeftValue, canonicalRightValue)
                canonicalLeftValue = v.first
                canonicalRightValue = v.second
                leftKind = k.first
                rightKind = k.second
            }
            // Any, Boolean|Null
            else if (rightKind == ValueKind.Boolean || rightKind == ValueKind.Null) {
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
            }

            return Double.NaN
        }

        private fun getKind(canonicalValue: Any?): ValueKind {
            if (canonicalValue == null) {
                return ValueKind.Null
            } else if (canonicalValue is Boolean) {
                return ValueKind.Boolean
            } else if (canonicalValue is Double) {
                return ValueKind.Number
            } else if (canonicalValue is String) {
                return ValueKind.String
            } else if (canonicalValue is IReadOnlyObject) {
                return ValueKind.Object
            } else if (canonicalValue is IReadOnlyArray<*>) {
                return ValueKind.Array
            }

            return ValueKind.Object
        }
    }
}
