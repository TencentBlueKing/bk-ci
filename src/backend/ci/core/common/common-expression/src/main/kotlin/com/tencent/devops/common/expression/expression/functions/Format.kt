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

package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.FunctionFormatException
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.MemoryCounter
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

@Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount")
class Format : Function() {
    companion object {
        const val name = "format"
    }

    override fun createNode() = Format()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val resultMemory = null
        val format = parameters[0].evaluate(context).convertToString()
        var index = 0
        val result = FormatResultBuilder(this, context, null)
        while (index < format.length) {
            val lbrace = format.indexOf('{', index)
            var rbrace = format.indexOf('}', index)

            // 左大括号
            if (lbrace >= 0 && (rbrace < 0 || rbrace > lbrace)) {
                // 如果有两个左大括号需要转义 {{ -> {
                if (safeCharAt(format, lbrace + 1) == '{') {
                    result.append(format.substring(index, lbrace + 1))
                    index = lbrace + 2
                    continue
                }
                // 寻找左括号，arg，格式化和右括号
                val (argIndex, endArgIndex, readArgIndexOk) = readArgIndex(format, lbrace + 1)
                val (formatSpecifiers, rbraceNew, readFormatSpecifiersOk) = readFormatSpecifiers(
                    format, endArgIndex + 1
                )
                rbrace = rbraceNew
                if (rbrace > lbrace + 1 && readArgIndexOk && readFormatSpecifiersOk) {
                    // 校验 arg的位数是否超过参数总数
                    if (argIndex!! > parameters.count() - 2) {
                        throw FunctionFormatException.invalidFormatArgIndex(format)
                    }

                    // 加上左括号前的
                    if (lbrace > index) {
                        result.append(format.substring(index, lbrace))
                    }

                    // 加上arg参数，以及调到右括号后
                    result.append(argIndex, formatSpecifiers)
                    index = rbrace + 1
                } else {
                    throw FunctionFormatException.invalidFormatString(format)
                }
            }
            // 右大括号
            else if (rbrace >= 0) {
                // 如果有两个右大括号需要转义 }} -> }
                if (safeCharAt(format, rbrace + 1) == '}') {
                    result.append(format.substring(index, rbrace + 1))
                    index = rbrace + 2
                } else {
                    throw FunctionFormatException.invalidFormatString(format)
                }
            }
            // 加上最后的右括号后的东西
            else {
                result.append(format.substring(index))
                break
            }
        }

        return Pair(resultMemory, result.toString())
    }

    // 寻找 {Arg}中 Arg的位置
    private fun readArgIndex(
        str: String,
        startIndex: Int
    ): Triple<Int?, Int, Boolean> {
        var length = 0
        while (safeCharAt(str, startIndex + length)?.isDigit() == true) {
            length++
        }

        if (length < 1) {
            return Triple(0, 0, false)
        }

        val endIndex = startIndex + length - 1
        val result = str.substring(startIndex, startIndex + length).toIntOrNull()
        return Triple(result, endIndex, result != null)
    }

    // 寻找格式化位置，目前在azure上有，github action文档上无。所以暂不支持
    // format('{0:yyyyMMdd}', pipeline.startTime)
    private fun readFormatSpecifiers(
        str: String,
        startIndex: Int
    ): Triple<String?, Int, Boolean> {
        var result: String? = null
        var rbrace = 0
        // No format specifiers
        var c = safeCharAt(str, startIndex)
        if (c == '}') {
            result = ""
            rbrace = startIndex
            return Triple(result, rbrace, true)
        }

        // Validate starts with ":"
        if (c != ':') {
            result = null
            rbrace = 0
            return Triple(result, rbrace, false)
        }

        // Read the specifiers
        val specifiers = StringBuilder()
        var index = startIndex + 1
        while (true) {
            // Validate not the end of the string
            if (index >= str.length) {
                result = null
                rbrace = 0
                return Triple(result, rbrace, false)
            }

            c = str[index]

            // Not right-brace
            if (c != '}') {
                specifiers.append(c)
                index++
            }
            // Escaped right-brace
            else if (safeCharAt(str, index + 1) == '}') {
                specifiers.append('}')
                index += 2
            }
            // Closing right-brace
            else {
                result = specifiers.toString()
                rbrace = index
                return Triple(result, rbrace, true)
            }
        }
    }

    private fun safeCharAt(
        str: String,
        index: Int
    ): Char? {
        if (str.length > index) {
            return str[index]
        }

        return null
    }

    private class FormatResultBuilder(
        private val node: Format,
        private val context: EvaluationContext,
        private val counter: MemoryCounter?
    ) {
        private var mCache: Array<ArgValue?> = Array(node.parameters.count() - 1) { null }
        private val mSegments = mutableListOf<Any>()

        override fun toString(): String {
            return mSegments.joinToString("") {
                if (it is Lazy<*>) {
                    it.value as String
                } else {
                    it as String
                }
            }
        }

        fun append(value: String?) {
            if (!value.isNullOrEmpty()) {
                counter?.add(value)
                mSegments.add(value)
            }
        }

        fun append(argIndex: Int, formatSpecifiers: String?) {
            mSegments.add(
                lazy<String> {
                    var result: String? = null
                    var argValue = mCache[argIndex]

                    if (argValue == null) {
                        val evaluationResult = node.parameters[argIndex + 1].evaluate(context)
                        val stringResult = evaluationResult.convertToString()
                        argValue = ArgValue(evaluationResult, stringResult)
                        mCache[argIndex] = argValue
                    }

                    // 在这里禁掉了使用format azure 0:yyyyMMdd 的可能
                    if (formatSpecifiers.isNullOrEmpty()) {
                        result = argValue.stringResult
                    } else {
                        throw FunctionFormatException.invalidFormatSpecifiers(
                            formatSpecifiers,
                            argValue.evaluationResult.kind
                        )
                    }

                    if (result.isNotEmpty()) {
                        counter?.add(result)
                    }

                    result
                }
            )
        }
    }

    private class ArgValue(
        val evaluationResult: EvaluationResult,
        val stringResult: String
    )

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val sb = StringBuilder(name).append("(")
        parameters.forEachIndexed { index, param ->
            sb.append(param.subNameValueEvaluate(context).parseSubNameValueEvaluateResult())
            if (index != parameters.count() - 1) {
                sb.append(", ")
            }
        }
        sb.append(")")
        return Pair(sb.toString(), false)
    }
}
