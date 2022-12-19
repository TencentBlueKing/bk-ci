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

import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.sdk.ExpressionUtility

@Suppress("ComplexCondition", "ComplexMethod", "LongMethod")
class LexicalAnalyzer(private val expression: String, val subNameValueEvaluateInfo: SubNameValueEvaluateInfo? = null) {
    private val mUnclosedTokens = ArrayDeque<Token?>()
    val unclosedTokens: Iterable<Token?>
        get() = mUnclosedTokens

    // 当前遍历到的字符
    private var mIndex: Int = 0

    // 上一个便利的 token
    private var mLastToken: Token? = null

    fun tryGetNextToken(): Pair<Token?, Boolean> {
        var rToken: Token? = null
        // 跳过空白符
        while (mIndex < expression.length && expression[mIndex].isWhitespace()) {
            mIndex++
        }

        // 判断是否是最后一个字符
        if (mIndex >= expression.length) {
            return Pair(rToken, false)
        }

        // 读取第一个字符来确定 token 的类型
        when (val c = expression[mIndex]) {
            // "("
            ExpressionConstants.START_GROUP -> {
                //  函数调用
                rToken = if (mLastToken?.kind == TokenKind.Function) {
                    createToken(TokenKind.StartParameters, c, mIndex++)
                }
                // Logical grouping
                else {
                    createToken(TokenKind.StartGroup, c, mIndex++)
                }
            }
            // "["
            ExpressionConstants.START_INDEX -> {
                rToken = createToken(TokenKind.StartIndex, c, mIndex++)
            }
            // ")"
            ExpressionConstants.END_GROUP -> {
                // Function call "(" function call
                if (mUnclosedTokens.peek()?.kind == TokenKind.StartParameters) {
                    rToken = createToken(TokenKind.EndParameters, c, mIndex++)
                }
                // Logical grouping
                else {
                    rToken = createToken(TokenKind.EndGroup, c, mIndex++)
                }
            }
            // "]"
            ExpressionConstants.END_INDEX -> {
                rToken = createToken(TokenKind.EndIndex, c, mIndex++)
            }
            // ","
            ExpressionConstants.SEPARATOR -> {
                rToken = createToken(TokenKind.Separator, c, mIndex++)
            }
            // "*"
            ExpressionConstants.WILDCARD -> {
                rToken = createToken(TokenKind.Wildcard, c, mIndex++)
            }

            '\'' -> {
                rToken = readStringToken()
            }
            // "!" and "!=", ">" and ">=", "<" and "<=", "==", "&&", "||"
            '!', '>', '<', '=', '&', '|' -> {
                rToken = readOperator()
            }

            else -> {
                if (c == '.') {
                    // Number
                    if (mLastToken == null ||
                        mLastToken?.kind == TokenKind.Separator || // ","
                        mLastToken?.kind == TokenKind.StartGroup || // "(" logical grouping
                        mLastToken?.kind == TokenKind.StartIndex || // "["
                        mLastToken?.kind == TokenKind.StartParameters || // "(" function call
                        mLastToken?.kind == TokenKind.LogicalOperator
                    ) // "!", "==", etc
                        {
                            rToken = readNumberToken()
                        }
                    // "."
                    else {
                        rToken = createToken(TokenKind.Dereference, c, mIndex++)
                    }
                } else if (c == '-' || c == '+' || (c in '0'..'9')) {
                    rToken = readNumberToken()
                } else if (c == '$' && subNameValueEvaluateInfo?.enableSubNameValueEvaluate == true) {
                    // 针对部分替换提供"$"
                    rToken = readSubValueExpressionToken()
                } else {
                    rToken = readKeywordToken()
                }
            }
        }

        mLastToken = rToken
        return Pair(rToken, true)
    }

    private fun readNumberToken(): Token {
        val startIndex = mIndex
        do {
            mIndex++
        } while (mIndex < expression.length && (!testTokenBoundary(expression[mIndex]) || expression[mIndex] == '.'))
        val length = mIndex - startIndex
        val str = expression.substring(startIndex, startIndex + length)
        val d = ExpressionUtility.parseNumber(str)
        return if (d.isNaN()) {
            createToken(TokenKind.Unexpected, str, startIndex)
        } else {
            createToken(TokenKind.Number, str, startIndex, d)
        }
    }

    private fun readKeywordToken(): Token {
        // Read to the end of the keyword.
        val startIndex = mIndex
        mIndex++ // Skip the first char. It is already known to be the start of the keyword.
        while (mIndex < expression.length && !testTokenBoundary(expression.get(mIndex))) {
            mIndex++
        }

        // Test if valid keyword character sequence.
        val length = mIndex - startIndex
        val str = expression.substring(startIndex, startIndex + length)
        return if (ExpressionUtility.isLegalKeyword(str)) {
            // Test if follows property dereference operator.
            if (mLastToken != null && mLastToken!!.kind == TokenKind.Dereference) {
                return createToken(TokenKind.PropertyName, str, startIndex)
            }

            // Null
            if (str == ExpressionConstants.NULL) {
                return createToken(TokenKind.Null, str, startIndex)
            } else if (str == ExpressionConstants.TRUE) {
                return createToken(TokenKind.Boolean, str, startIndex, true)
            } else if (str == ExpressionConstants.FALSE) {
                return createToken(TokenKind.Boolean, str, startIndex, false)
            } else if (str == ExpressionConstants.NAN) {
                return createToken(TokenKind.Number, str, startIndex, Double.NaN)
            } else if (str == ExpressionConstants.INFINITY) {
                return createToken(TokenKind.Number, str, startIndex, Double.POSITIVE_INFINITY)
            }

            // Lookahead
            var tempIndex = mIndex
            while (tempIndex < expression.length && expression[tempIndex].isWhitespace()) {
                tempIndex++
            }

            // Function
            if (tempIndex < expression.length && expression[tempIndex] == ExpressionConstants.START_GROUP) // "("
                {
                    createToken(TokenKind.Function, str, startIndex)
                } else {
                createToken(TokenKind.NamedValue, str, startIndex)
            }
        } else {
            // Invalid keyword
            createToken(TokenKind.Unexpected, str, startIndex)
        }
    }

    private fun readStringToken(): Token {
        val startIndex = mIndex
        var c: Char
        var closed = false
        val str = StringBuilder()
        mIndex++ // Skip the leading single-quote.
        while (mIndex < expression.length) {
            c = expression[mIndex++]
            if (c == '\'') {
                // End of string.
                if (mIndex >= expression.length || expression[mIndex] != '\'') {
                    closed = true
                    break
                }

                // Escaped single quote.
                mIndex++
            }

            str.append(c)
        }

        val length = mIndex - startIndex
        val rawValue = expression.substring(startIndex, startIndex + length)
        if (closed) {
            return createToken(TokenKind.String, rawValue, startIndex, str.toString())
        }

        return createToken(TokenKind.Unexpected, rawValue, startIndex)
    }

    /**
     * 针对subNameValue部分替换提供
     * 用于支持在表达式中支持部分替换为另一个表达式如 ${{ ${{ }}  }}
     */
    private fun readSubValueExpressionToken(): Token {
        val startIndex = mIndex
        var c: Char
        var closed = false
        val str = StringBuilder()
        mIndex++ // Skip the leading single-quote.
        while (mIndex < expression.length) {
            // 校验${{
            if ((mIndex == startIndex + 1 && expression[mIndex] != '{') ||
                (mIndex == startIndex + 2 && expression[mIndex] != '{')
            ) {
                return createToken(TokenKind.Unexpected, expression.substring(startIndex, mIndex), startIndex)
            }

            c = expression[mIndex++]

            str.append(c)

            if (str.endsWith("}}")) {
                closed = true
                break
            }
        }

        val length = mIndex - startIndex
        val rawValue = expression.substring(startIndex, startIndex + length)
        if (closed) {
            return createToken(TokenKind.Expression, rawValue, startIndex, str.toString())
        }

        return createToken(TokenKind.Unexpected, rawValue, startIndex)
    }

    private fun readOperator(): Token {
        val startIndex = mIndex
        var raw: String?
        mIndex++

        // Check for a two-character operator
        if (mIndex < expression.length) {
            mIndex++
            raw = expression.substring(startIndex, startIndex + 2)
            when (raw) {
                ExpressionConstants.NOT_EQUAL,
                ExpressionConstants.GREATER_THAN_OR_EQUAL,
                ExpressionConstants.LESS_THAN_OR_EQUAL,
                ExpressionConstants.EQUAL,
                ExpressionConstants.AND,
                ExpressionConstants.OR ->
                    return createToken(TokenKind.LogicalOperator, raw, startIndex)
            }

            // Backup
            mIndex--
        }

        // Check for one-character operator
        raw = expression.substring(startIndex, startIndex + 1)
        when (raw) {
            ExpressionConstants.NOT,
            ExpressionConstants.GREATER_THAN,
            ExpressionConstants.LESS_THAN ->
                return createToken(TokenKind.LogicalOperator, raw, startIndex)
        }

        // Unexpected
        while (mIndex < expression.length && !testTokenBoundary(expression[mIndex])) {
            mIndex++
        }

        val length = mIndex - startIndex
        raw = expression.substring(startIndex, startIndex + length)
        return createToken(TokenKind.Unexpected, raw, startIndex)
    }

    private fun createToken(
        kind: TokenKind,
        rawValue: Char,
        index: Int,
        parsedValue: Any? = null
    ): Token {
        return createToken(kind, rawValue.toString(), index, parsedValue)
    }

    private fun createToken(
        kind: TokenKind,
        rawValue: String,
        index: Int,
        parsedValue: Any? = null
    ): Token {
        // 根据上一个token判断当前token是否合法
        var legal = false
        when (kind) {
            // "(" logical grouping
            TokenKind.StartGroup -> {
                // 是第一个还是在“，”或“（”或“[”或逻辑运算符之后
                legal = checkLastToken(
                    null,
                    TokenKind.Separator,
                    TokenKind.StartGroup,
                    TokenKind.StartParameters,
                    TokenKind.StartIndex,
                    TokenKind.LogicalOperator
                )
            }
            // "["
            TokenKind.StartIndex -> {
                // Follows ")", "]", "*", a property name, or a named-value
                legal = checkLastToken(
                    TokenKind.EndGroup,
                    TokenKind.EndParameters,
                    TokenKind.EndIndex,
                    TokenKind.Wildcard,
                    TokenKind.PropertyName,
                    TokenKind.NamedValue
                )
            }
            // "(" function call
            TokenKind.StartParameters -> {
                // Follows a function
                legal = checkLastToken(TokenKind.Function)
            }
            // ")" logical grouping
            TokenKind.EndGroup -> {
                // Follows ")", "]", "*", a literal, a property name, or a named-value
                legal = checkLastToken(
                    TokenKind.EndGroup,
                    TokenKind.EndParameters,
                    TokenKind.EndIndex,
                    TokenKind.Wildcard,
                    TokenKind.Null,
                    TokenKind.Boolean,
                    TokenKind.Number,
                    TokenKind.String,
                    TokenKind.PropertyName,
                    TokenKind.NamedValue
                )
            }
            // "]"
            TokenKind.EndIndex -> {
                // Follows ")", "]", "*", a literal, a property name, or a named-value
                legal = checkLastToken(
                    TokenKind.EndGroup,
                    TokenKind.EndParameters,
                    TokenKind.EndIndex,
                    TokenKind.Wildcard,
                    TokenKind.Null,
                    TokenKind.Boolean,
                    TokenKind.Number,
                    TokenKind.String,
                    TokenKind.PropertyName,
                    TokenKind.NamedValue
                )
            }
            // ")" function call
            TokenKind.EndParameters -> {
                // Follows "(" function call, ")", "]", "*", a literal, a property name, or a named-value
                legal = checkLastToken(
                    TokenKind.StartParameters,
                    TokenKind.EndGroup,
                    TokenKind.EndParameters,
                    TokenKind.EndIndex,
                    TokenKind.Wildcard,
                    TokenKind.Null,
                    TokenKind.Boolean,
                    TokenKind.Number,
                    TokenKind.String,
                    TokenKind.PropertyName,
                    TokenKind.NamedValue
                )
            }
            // ","
            TokenKind.Separator -> {
                // Follows ")", "]", "*", a literal, a property name, or a named-value
                legal = checkLastToken(
                    TokenKind.EndGroup,
                    TokenKind.EndParameters,
                    TokenKind.EndIndex,
                    TokenKind.Wildcard,
                    TokenKind.Null,
                    TokenKind.Boolean,
                    TokenKind.Number,
                    TokenKind.String,
                    TokenKind.PropertyName,
                    TokenKind.NamedValue
                )
            }
            // "."
            TokenKind.Dereference -> {
                // Follows ")", "]", "*", a property name, or a named-value
                legal = checkLastToken(
                    TokenKind.EndGroup,
                    TokenKind.EndParameters,
                    TokenKind.EndIndex,
                    TokenKind.Wildcard,
                    TokenKind.PropertyName,
                    TokenKind.NamedValue
                )
            }
            // "*"
            TokenKind.Wildcard -> {
                // Follows "[" or "."
                legal = checkLastToken(TokenKind.StartIndex, TokenKind.Dereference)
            }
            // "!", "==", etc
            TokenKind.LogicalOperator -> {
                when (rawValue) {
                    ExpressionConstants.NOT -> {
                        // Is first or follows "," or "(" or "[" or a logical operator
                        legal = checkLastToken(
                            null,
                            TokenKind.Separator,
                            TokenKind.StartGroup,
                            TokenKind.StartParameters,
                            TokenKind.StartIndex,
                            TokenKind.LogicalOperator
                        )
                    }

                    else -> {
                        // Follows ")", "]", "*", a literal, a property name, or a named-value
                        legal = checkLastToken(
                            TokenKind.EndGroup,
                            TokenKind.EndParameters,
                            TokenKind.EndIndex,
                            TokenKind.Wildcard,
                            TokenKind.Null,
                            TokenKind.Boolean,
                            TokenKind.Number,
                            TokenKind.String,
                            TokenKind.PropertyName,
                            TokenKind.NamedValue
                        )
                    }
                }
            }

            TokenKind.Null, TokenKind.Boolean, TokenKind.Number, TokenKind.String -> {
                // Is first or follows "," or "[" or "(" or a logical operator (e.g. "!" or "==" etc)
                legal = checkLastToken(
                    null,
                    TokenKind.Separator,
                    TokenKind.StartIndex,
                    TokenKind.StartGroup,
                    TokenKind.StartParameters,
                    TokenKind.LogicalOperator
                )
            }

            // 针对subNameValue特供，在expression后都是合法的
            TokenKind.Expression -> {
                legal = true
            }

            TokenKind.PropertyName -> {
                // Follows "."
                legal = checkLastToken(TokenKind.Dereference)
            }

            TokenKind.Function -> {
                // Is first or follows "," or "[" or "(" or a logical operator (e.g. "!" or "==" etc)
                legal = checkLastToken(
                    null,
                    TokenKind.Separator,
                    TokenKind.StartIndex,
                    TokenKind.StartGroup,
                    TokenKind.StartParameters,
                    TokenKind.LogicalOperator
                )
            }

            TokenKind.NamedValue -> {
                // Is first or follows "," or "[" or "(" or a logical operator (e.g. "!" or "==" etc)
                legal = checkLastToken(
                    null,
                    TokenKind.Separator,
                    TokenKind.StartIndex,
                    TokenKind.StartGroup,
                    TokenKind.StartParameters,
                    TokenKind.LogicalOperator
                )
            }
        }

        if (!legal) {
            return Token(TokenKind.Unexpected, rawValue, index)
        }

        val token = Token(kind, rawValue, index, parsedValue)

        when (kind) {
            // "(" logical grouping, "[", "(" function call
            TokenKind.StartGroup, TokenKind.StartIndex, TokenKind.StartParameters -> {
                // Track start token
                mUnclosedTokens.push(token)
            }
            // ")" logical grouping
            TokenKind.EndGroup -> {
                // Check inside logical grouping
                if (mUnclosedTokens.peek()?.kind != TokenKind.StartGroup) {
                    return Token(TokenKind.Unexpected, rawValue, index)
                }

                // Pop start token
                mUnclosedTokens.pop()
            }
            // "]"
            TokenKind.EndIndex -> {
                // Check inside indexer
                if (mUnclosedTokens.peek()?.kind != TokenKind.StartIndex) {
                    return Token(TokenKind.Unexpected, rawValue, index)
                }

                // Pop start token
                mUnclosedTokens.pop()
            }
            // ")" function call
            TokenKind.EndParameters -> {
                // Check inside function call
                if (mUnclosedTokens.peek()?.kind != TokenKind.StartParameters) {
                    return Token(TokenKind.Unexpected, rawValue, index)
                }

                // Pop start token
                mUnclosedTokens.pop()
            }
            // ","
            TokenKind.Separator -> {
                // Check inside function call
                if (mUnclosedTokens.peek()?.kind != TokenKind.StartParameters) {
                    return Token(TokenKind.Unexpected, rawValue, index)
                }
            }
        }

        return token
    }

    /**
     * 检查上一个 lastToken kind 是否在允许种类的数组中。
     */
    private fun checkLastToken(vararg allowed: TokenKind?): Boolean {
        val lastKind = mLastToken?.kind

        // 针对subNameValue特供，表达式token可以插入到任何地方
        if (lastKind == TokenKind.Expression) {
            return true
        }

        allowed.forEach { kind ->
            if (kind == lastKind) {
                return true
            }
        }

        return false
    }

    companion object {
        private fun testTokenBoundary(c: Char): Boolean {
            when (c) {
                ExpressionConstants.START_GROUP, // "("
                ExpressionConstants.START_INDEX, // "["
                ExpressionConstants.END_GROUP, // ")"
                ExpressionConstants.END_INDEX, // "]"
                ExpressionConstants.SEPARATOR, // ","
                ExpressionConstants.DEREFERENCE, // "."
                '!', // "!" and "!="
                '>', // ">" and ">="
                '<', // "<" and "<="
                '=', // "=="
                '&', // "&&"
                '|' // "||"
                -> return true

                else -> return c.isWhitespace()
            }
        }
    }
}
