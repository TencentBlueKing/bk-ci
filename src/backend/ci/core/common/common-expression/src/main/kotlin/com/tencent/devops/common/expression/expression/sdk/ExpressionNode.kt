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

import com.tencent.devops.common.expression.DistinguishType
import com.tencent.devops.common.expression.NotSupportedException
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.EvaluationOptions
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.IExpressionNode
import com.tencent.devops.common.expression.expression.ITraceWriter
import com.tencent.devops.common.expression.expression.ValueKind
import com.tencent.devops.common.expression.utils.ExpressionJsonUtil
import kotlin.math.floor

@Suppress("ComplexMethod", "ReturnCount")
abstract class ExpressionNode : IExpressionNode {

    var container: Container? = null

    var level: Int = 0
        private set

    private var mName: String? = null

    var name: String
        get() = if (!mName.isNullOrBlank()) {
            mName!!
        } else {
            this::class.java.name
        }
        set(value) {
            mName = value
        }

    protected abstract val traceFullyRealized: Boolean

    override fun evaluate(trace: ITraceWriter?, state: Any?, options: EvaluationOptions?): EvaluationResult {
        if (container != null) {
            throw NotSupportedException("Expected IExpressionNode.Evaluate to be called on root node only.")
        }

        val eTrace = EvaluationTraceWriter(trace)
        val context = EvaluationContext(eTrace, state, options, this)
        eTrace.info("Evaluating: ${convertToExpression()}")
        val result = evaluate(context)

        // Trace the result
        traceTreeResult(context, result.value, result.kind)

        return result
    }

    fun evaluate(context: EvaluationContext): EvaluationResult {
        // Evaluate
        level = if (container == null) {
            0
        } else {
            container!!.level + 1
        }
        traceVerbose(context, level, "Evaluating $name:")
        var (coreMemory, coreResult) = evaluateCore(context)

        if (coreMemory == null) {
            coreMemory = ResultMemory()
        }

        // Convert to canonical value
        val (kind, raw, value) = ExpressionUtility.convertToCanonicalValue(coreResult)

        // The depth can be safely trimmed when the total size of the core result is known,
        // or when the total size of the core result can easily be determined.
        val trimDepth = coreMemory.isTotal || (raw == null && ExpressionUtility.isPrimitive(kind))

        // Account for the memory overhead of the core result
        val coreBytes = coreMemory.bytes ?: EvaluationMemory.calculateBytes(raw ?: value)
        context.memory.addAmount(level, coreBytes, trimDepth)

        // Account for the memory overhead of the conversion result
        if (raw != null) {
            val conversionBytes = EvaluationMemory.calculateBytes(value)
            context.memory.addAmount(level, conversionBytes)
        }

        val result = EvaluationResult(context, level, value, kind, raw)

        // Store the trace result
        if (this.traceFullyRealized) {
            context.setTraceResult(this, result)
        }

        return result
    }

    abstract fun convertToExpression(): String

    abstract fun convertToRealizedExpression(context: EvaluationContext): String

    protected abstract fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?>

    override fun subNameValueEvaluate(
        trace: ITraceWriter?,
        state: Any?,
        options: EvaluationOptions?,
        subInfo: SubNameValueEvaluateInfo
    ): Pair<String, Boolean> {
        if (subInfo.hasOtherNameValue) {
            // 目前部分计算不涉及内存计算，未来启用内存计算时需要修改此处
            if (container != null) {
                throw NotSupportedException("Expected IExpressionNode.Evaluate to be called on root node only.")
            }
            val eTrace = EvaluationTraceWriter(trace)
            val context = EvaluationContext(eTrace, state, options, this)

            return Pair(subNameValueEvaluate(context), false)
        }

        val re = evaluate(null, state, null).value
        // 计算结果是流水线上下文
        if (re is PipelineContextData) {
            val res = when (re) {
                is StringContextData -> DistinguishType.STRING.distinguishByType(
                    re.getString(), subInfo.distinguishTypes
                )
                is BooleanContextData -> DistinguishType.BOOL.distinguishByType(
                    re.getBoolean().toString(), subInfo.distinguishTypes
                )
                is NumberContextData -> DistinguishType.NUMBER.distinguishByType(
                    numberToString(re.value), subInfo.distinguishTypes
                )
                is ArrayContextData -> DistinguishType.ARRAY.distinguishByType(
                    ExpressionJsonUtil.getObjectMapper().writeValueAsString(re.toJson()), subInfo.distinguishTypes
                )
                is DictionaryContextData -> DistinguishType.DICT.distinguishByType(
                    ExpressionJsonUtil.getObjectMapper().writeValueAsString(re.toJson()), subInfo.distinguishTypes
                )
                else -> ExpressionJsonUtil.getObjectMapper().writeValueAsString(re.toJson())
            }
            return Pair(res, true)
        }
        val res = when (re) {
            is Char, is String -> DistinguishType.STRING.distinguishByType(re.toString(), subInfo.distinguishTypes)
            is Number -> DistinguishType.NUMBER.distinguishByType(numberToString(re), subInfo.distinguishTypes)
            is Boolean -> DistinguishType.BOOL.distinguishByType(re.toString(), subInfo.distinguishTypes)
            else -> re?.toString() ?: ""
        }
        return Pair(res, true)
    }

    private fun DistinguishType.distinguishByType(
        value: String,
        distinguishTypes: Set<DistinguishType>?
    ): String {
        if (distinguishTypes == null) {
            return value
        }
        if (this in distinguishTypes) {
            return "'$value'"
        }
        return value
    }

    fun subNameValueEvaluate(context: EvaluationContext): String {
        val result = subNameValueEvaluateCore(context) ?: return "''"
        // 在subNameValue的情况下只有指定的nameValued才可能返回这个
        if (result is PipelineContextData) {
            if (result is BooleanContextData || result is NumberContextData) {
                return ExpressionJsonUtil.getObjectMapper().writeValueAsString(result.toJson())
            }
            return "'${ExpressionJsonUtil.getObjectMapper().writeValueAsString(result.toJson())}'"
        }
        return result.toString()
    }

    protected abstract fun subNameValueEvaluateCore(context: EvaluationContext): Any?

    private fun traceTreeResult(
        context: EvaluationContext,
        result: Any?,
        kind: ValueKind
    ) {
        // Get the realized expression
        val realizedExpression = convertToRealizedExpression(context)

        // Format the result
        val traceValue = ExpressionUtility.formatValue(result, kind)

        // Only trace the realized expression if it is meaningfully different
        if (realizedExpression != traceValue) {
            if (kind == ValueKind.Number && realizedExpression == "'$traceValue'") {
                // Don't bother tracing the realized expression when the result is a number and the
                // realized expresion is a precisely matching string.
            } else {
                context.trace?.info("Expanded: $realizedExpression")
            }
        }

        // Always trace the result
        context.trace?.info("Result: $traceValue")
    }

    companion object {
        private fun numberToString(v: Number): String {
            val value = v.toDouble()
            if (value.isNaN() || value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY) {
                return value.toString()
            }

            val floored = floor(value)
            return if (value == floored && value <= Int.MAX_VALUE && value >= Int.MIN_VALUE) {
                val flooredInt = floored.toInt()
                flooredInt.toString()
            } else if (value == floored && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
                val flooredInt = floored.toLong()
                flooredInt.toString()
            } else {
                value.toString()
            }
        }

        private fun traceVerbose(context: EvaluationContext, level: Int, message: String?) {
            context.trace?.verbose("".padStart(level * 2, '.') + (message ?: ""))
        }
    }
}
