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

package com.tencent.devops.common.expression.expression.tokens

import com.tencent.devops.common.expression.NotSupportedException
import com.tencent.devops.common.expression.context.ExpressionValueNode
import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.sdk.ExpressionNode
import com.tencent.devops.common.expression.expression.sdk.Literal
import com.tencent.devops.common.expression.expression.sdk.Wildcard
import com.tencent.devops.common.expression.expression.sdk.operators.And
import com.tencent.devops.common.expression.expression.sdk.operators.Equal
import com.tencent.devops.common.expression.expression.sdk.operators.GreaterThan
import com.tencent.devops.common.expression.expression.sdk.operators.GreaterThanOrEqual
import com.tencent.devops.common.expression.expression.sdk.operators.Index
import com.tencent.devops.common.expression.expression.sdk.operators.LessThan
import com.tencent.devops.common.expression.expression.sdk.operators.LessThanOrEqual
import com.tencent.devops.common.expression.expression.sdk.operators.Not
import com.tencent.devops.common.expression.expression.sdk.operators.NotEqual
import com.tencent.devops.common.expression.expression.sdk.operators.Or

// 词法分析中的 标记，是一个字符串，也是构成源代码的最小单位
@Suppress("ComplexMethod")
class Token(
    val kind: TokenKind,
    val rawValue: String,
    val index: Int,
    val parsedValue: Any? = null
) {

    val associativity: Associativity
        get() {
            when (kind) {
                TokenKind.StartGroup -> Associativity.None
                TokenKind.LogicalOperator -> {
                    when (rawValue) {
                        ExpressionConstants.NOT -> return Associativity.RightToLeft
                    }
                }
            }

            return if (isOperator) {
                Associativity.LeftToRight
            } else {
                Associativity.None
            }
        }

    val isOperator: Boolean
        get() {
            return when (kind) {
                TokenKind.StartGroup, // "(" logical grouping
                TokenKind.StartIndex, // "["
                TokenKind.StartParameters, // "(" function call
                TokenKind.EndGroup, // ")" logical grouping
                TokenKind.EndIndex, // "]"
                TokenKind.EndParameters, // ")" function call
                TokenKind.Separator, // ","
                TokenKind.Dereference, // "."
                TokenKind.LogicalOperator -> // "!", "==", etc
                    true
                else ->
                    false
            }
        }

    // 运算符优先级，只对 operator tokens 有意义
    val precedence: Int
        get() {
            when (kind) {
                TokenKind.StartGroup -> return 20 // "(" logical grouping
                TokenKind.StartIndex, // "["
                TokenKind.StartParameters, // "(" function call
                TokenKind.Dereference -> // "."
                    return 19
                TokenKind.LogicalOperator -> {
                    when (rawValue) {
                        ExpressionConstants.NOT -> return 16; // "!"
                        ExpressionConstants.GREATER_THAN, // ">"
                        ExpressionConstants.GREATER_THAN_OR_EQUAL, // ">="
                        ExpressionConstants.LESS_THAN, // "<"
                        ExpressionConstants.LESS_THAN_OR_EQUAL -> // "<="
                            return 11
                        ExpressionConstants.EQUAL, // "=="
                        ExpressionConstants.NOT_EQUAL -> // "!="
                            return 10
                        ExpressionConstants.AND -> // "&&"
                            return 6
                        ExpressionConstants.OR -> // "||"
                            return 5
                    }
                }
                TokenKind.EndGroup, // ")" logical grouping
                TokenKind.EndIndex, // "]"
                TokenKind.EndParameters, // ")" function call
                TokenKind.Separator -> // ","
                    return 1
            }

            return 0
        }

    /**
     * Expected number of operands. The value is only meaningful for standalone unary operators and binary operators.
     */
    val operandCount: Int
        get() {
            when (kind) {
                // "[" // "."
                TokenKind.StartIndex, TokenKind.Dereference -> return 2
                TokenKind.LogicalOperator -> {
                    when (rawValue) {

                        ExpressionConstants.NOT -> return 1; // "!"
                        ExpressionConstants.GREATER_THAN, // ">"
                        ExpressionConstants.GREATER_THAN_OR_EQUAL, // ">="
                        ExpressionConstants.LESS_THAN, // "<"
                        ExpressionConstants.LESS_THAN_OR_EQUAL, // "<="
                        ExpressionConstants.EQUAL, // "=="
                        ExpressionConstants.NOT_EQUAL, // "!="
                        ExpressionConstants.AND, // "&&"
                        ExpressionConstants.OR -> // "|"
                            return 2
                    }
                }
            }
            return 0
        }

    fun toNode(): ExpressionNode {
        when (kind) {
            // "[","."
            TokenKind.StartIndex, TokenKind.Dereference -> return Index()

            TokenKind.LogicalOperator -> when (rawValue) {
                // "!"
                ExpressionConstants.NOT -> return Not()
                // "!="
                ExpressionConstants.NOT_EQUAL -> return NotEqual()
                // ">"
                ExpressionConstants.GREATER_THAN -> return GreaterThan()
                // ">="
                ExpressionConstants.GREATER_THAN_OR_EQUAL -> return GreaterThanOrEqual()
                // "<"
                ExpressionConstants.LESS_THAN -> return LessThan()
                // "<="
                ExpressionConstants.LESS_THAN_OR_EQUAL -> return LessThanOrEqual()
                // "=="
                ExpressionConstants.EQUAL -> return Equal()
                // "&&"
                ExpressionConstants.AND -> return And()
                // "||"
                ExpressionConstants.OR -> return Or()

                else ->
                    throw NotSupportedException("Unexpected logical operator '$rawValue' when creating node")
            }

            TokenKind.Null, TokenKind.Boolean, TokenKind.Number, TokenKind.String -> return Literal(parsedValue)

            TokenKind.PropertyName -> return Literal(rawValue)
            // "*"
            TokenKind.Wildcard -> return Wildcard()

            TokenKind.Expression -> return ExpressionValueNode(rawValue)
        }

        throw NotSupportedException("Unexpected kind '$kind' when creating node")
    }
}

fun <T> ArrayDeque<T>.push(element: T) {
    this.addLast(element)
}

fun <T> ArrayDeque<T>.pop(): T {
    return this.removeLast()
}

fun <T> ArrayDeque<T>.peek(): T {
    return this.last()
}
