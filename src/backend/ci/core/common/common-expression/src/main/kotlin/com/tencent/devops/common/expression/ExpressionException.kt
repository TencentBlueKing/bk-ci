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
import com.tencent.devops.common.expression.expression.ParseExceptionKind
import com.tencent.devops.common.expression.expression.tokens.Token

@Suppress("UnnecessaryAbstractClass")
abstract class ExpressionException(message: String = "") : RuntimeException(message)

class NotSupportedException(override val message: String) : ExpressionException()

class ExpressionParseException(
    val kind: ParseExceptionKind,
    val token: Token?,
    val expression: String,
    override var message: String = ""
) : ExpressionException() {
    init {
        val desc = when (kind) {
            ParseExceptionKind.ExceededMaxDepth -> "Exceeded max expression depth ${ExpressionConstants.MAX_DEEP}"
            ParseExceptionKind.ExceededMaxLength -> "Exceeded max expression length ${ExpressionConstants.MAX_LENGTH}"
            ParseExceptionKind.TooFewParameters -> "Too few parameters supplied"
            ParseExceptionKind.TooManyParameters -> "Too many parameters supplied"
            ParseExceptionKind.UnexpectedEndOfExpression -> "Unexpected end of expression"
            ParseExceptionKind.UnexpectedSymbol -> "Unexpected symbol"
            ParseExceptionKind.UnrecognizedFunction -> "Unrecognized function"
            ParseExceptionKind.UnrecognizedNamedValue -> "Unrecognized named-value"
        }

        message = if (token == null) {
            desc
        } else {
            "$desc: '${token.rawValue}'. Located at position ${token.index + 1} within expression: $expression"
        }
    }
}

class InvalidOperationException(override val message: String) : ExpressionException()

class ContextDataRuntimeException(override val message: String) : ExpressionException()

class FunctionFormatException(override val message: String?) : ExpressionException() {
    companion object {
        fun invalidFormatArgIndex(arg0: Any) = FunctionFormatException(
            "The following format string references more arguments than were supplied: $arg0"
        )

        fun invalidFormatSpecifiers(arg0: Any, arg1: Any) = FunctionFormatException(
            "The format specifiers '$arg0' are not valid for objects of type '$arg1'"
        )

        fun invalidFormatString(arg0: Any) = FunctionFormatException(
            "The following format string is invalid: $arg0"
        )
    }
}

class ContextNotFoundException(override val message: String?) : ExpressionException() {
    companion object {
        fun contextNameNotFound(arg0: String) = ContextNotFoundException(
            "Expression context $arg0 not found."
        )
    }
}
