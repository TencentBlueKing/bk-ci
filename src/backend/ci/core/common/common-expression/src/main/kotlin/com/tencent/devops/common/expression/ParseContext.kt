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

package com.tencent.devops.common.expression

import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.IFunctionInfo
import com.tencent.devops.common.expression.expression.INamedValueInfo
import com.tencent.devops.common.expression.expression.ITraceWriter
import com.tencent.devops.common.expression.expression.ParseExceptionKind
import com.tencent.devops.common.expression.expression.sdk.ExpressionNode
import com.tencent.devops.common.expression.expression.tokens.LexicalAnalyzer
import com.tencent.devops.common.expression.expression.tokens.Token
import java.util.TreeMap

class ParseContext(
    val expression: String,
    trace: ITraceWriter?,
    namedValues: Iterable<INamedValueInfo>?,
    functions: Iterable<IFunctionInfo>?,
    val allowUnknownKeywords: Boolean = false,
    // 针对部分替换的选项，会有一些特殊处理
    val subNameValueEvaluateInfo: SubNameValueEvaluateInfo? = null
) {
    val extensionFunctions = TreeMap<String, IFunctionInfo>(String.CASE_INSENSITIVE_ORDER)
    val extensionNamedValues = TreeMap<String, INamedValueInfo>()
    val lexicalAnalyzer: LexicalAnalyzer
    val operands = ArrayDeque<ExpressionNode>()
    val operators = ArrayDeque<Token>()
    val trace: ITraceWriter
    var token: Token? = null
    var lastToken: Token? = null

    init {
        if (expression.length > ExpressionConstants.MAX_LENGTH) {
            throw ExpressionParseException(ParseExceptionKind.ExceededMaxLength, null, expression)
        }

        namedValues?.forEach { namedValueInfo ->
            extensionNamedValues[namedValueInfo.name] = namedValueInfo
        }

        functions?.forEach { functionInfo ->
            extensionFunctions[functionInfo.name] = functionInfo
        }

        this.trace = trace ?: NoOperationTraceWriter()

        lexicalAnalyzer = LexicalAnalyzer(expression, subNameValueEvaluateInfo)
    }

    private class NoOperationTraceWriter : ITraceWriter {
        override fun info(message: String?) = Unit
        override fun verbose(message: String?) = Unit
    }
}
