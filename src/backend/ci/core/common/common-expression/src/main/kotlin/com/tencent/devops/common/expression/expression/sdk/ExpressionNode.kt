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
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.SubNameValueEvaluateResult
import com.tencent.devops.common.expression.SubNameValueResultType
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.ExpressionContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.EvaluationOptions
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.ExpressionOutput
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

    override fun evaluate(
        trace: ITraceWriter?,
        state: Any?,
        options: EvaluationOptions?,
        expressionOutput: ExpressionOutput?
    ): EvaluationResult {
        if (container != null) {
            throw NotSupportedException("Expected IExpressionNode.Evaluate to be called on root node only.")
        }

        val eTrace = EvaluationTraceWriter(trace)
        val context = EvaluationContext(eTrace, state, options, this, expressionOutput)
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
        subInfo: SubNameValueEvaluateInfo,
        expressionOutput: ExpressionOutput?
    ): SubNameValueEvaluateResult {
        // 目前部分计算不涉及内存计算，未来启用内存计算时需要修改此处
        if (container != null) {
            throw NotSupportedException("Expected IExpressionNode.Evaluate to be called on root node only.")
        }
        val eTrace = EvaluationTraceWriter(trace)
        val context = EvaluationContext(eTrace, state, options, this, expressionOutput)

        return subNameValueEvaluate(context)
    }

    fun subNameValueEvaluate(context: EvaluationContext): SubNameValueEvaluateResult {
        val (result, isComplete) = subNameValueEvaluateCore(context)
        if (result == null) {
            return SubNameValueEvaluateResult("", true, SubNameValueResultType.STRING)
        }
        // 计算结果是流水线上下文
        if (result is PipelineContextData) {
            return when (result) {
                is ExpressionContextData -> SubNameValueEvaluateResult(
                    value = result.getString(),
                    isComplete = isComplete,
                    type = SubNameValueResultType.EXPRESSION
                )

                is StringContextData -> SubNameValueEvaluateResult(
                    value = result.getString(),
                    isComplete = isComplete,
                    type = SubNameValueResultType.STRING
                )

                is BooleanContextData -> SubNameValueEvaluateResult(
                    value = result.getBoolean().toString(),
                    isComplete = isComplete,
                    type = SubNameValueResultType.BOOL
                )

                is NumberContextData -> SubNameValueEvaluateResult(
                    value = numberToString(result.value),
                    isComplete = isComplete,
                    type = SubNameValueResultType.NUMBER
                )

                // 对于array和dic因为subNameValue不涉及计算，所以将 " 转义为 \" 方便后续字符串转换
                is ArrayContextData -> SubNameValueEvaluateResult(
                    value = ExpressionJsonUtil.getObjectMapper().writeValueAsString(result.toJson())
                        .replace("\"", "\\\""),
                    isComplete = isComplete,
                    type = SubNameValueResultType.ARRAY
                )

                is DictionaryContextData -> SubNameValueEvaluateResult(
                    value = ExpressionJsonUtil.getObjectMapper().writeValueAsString(result.toJson())
                        .replace("\"", "\\\""),
                    isComplete = isComplete,
                    type = SubNameValueResultType.DICT
                )

                else -> SubNameValueEvaluateResult(
                    value = ExpressionJsonUtil.getObjectMapper().writeValueAsString(result.toJson()),
                    isComplete = isComplete,
                    type = SubNameValueResultType.STRING
                )
            }
        }
        return when (result) {
            is Char, is String -> {
                // 区分出表达式类型
                val res = result.toString()
                if (res.trim().startsWith("\${{") && res.endsWith("}}")) {
                    SubNameValueEvaluateResult(
                        value = res,
                        isComplete = isComplete,
                        type = SubNameValueResultType.EXPRESSION
                    )
                } else {
                    SubNameValueEvaluateResult(
                        value = res,
                        isComplete = isComplete,
                        type = SubNameValueResultType.STRING
                    )
                }
            }

            is Number -> SubNameValueEvaluateResult(
                value = numberToString(result),
                isComplete = isComplete,
                type = SubNameValueResultType.NUMBER
            )

            is Boolean -> SubNameValueEvaluateResult(
                value = result.toString(),
                isComplete = isComplete,
                type = SubNameValueResultType.BOOL
            )

            else -> SubNameValueEvaluateResult(
                value = result.toString(),
                isComplete = isComplete,
                type = SubNameValueResultType.STRING
            )
        }
    }

    /**
     * 方便表达式节点使用
     * 对于完全替换出的结果，如果是 字符串，列表JSON，数组JSON 都需要加单引号，方便后续使用
     * 其余结果直接返回
     */
    fun SubNameValueEvaluateResult.parseSubNameValueEvaluateResult(): String {
        if (!this.isComplete) {
            return this.value
        }
        return when (this.type) {
            SubNameValueResultType.STRING, SubNameValueResultType.DICT, SubNameValueResultType.ARRAY -> {
                "'$value'"
            }

            else -> value
        }
    }

    /**
     * subNameValue计算接口
     * 只要涉及到函数，运算符等一定是不完全计算，保留算式原文。
     * 单独的变量替换或者数值替换则是完全计算
     * @return <值，是否完全计算>
     */
    protected abstract fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean>

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
