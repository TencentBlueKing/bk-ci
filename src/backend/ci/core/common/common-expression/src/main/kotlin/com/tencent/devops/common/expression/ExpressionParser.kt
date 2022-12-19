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

import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.IExpressionNode
import com.tencent.devops.common.expression.expression.IFunctionInfo
import com.tencent.devops.common.expression.expression.INamedValueInfo
import com.tencent.devops.common.expression.expression.ITraceWriter
import com.tencent.devops.common.expression.expression.ParseExceptionKind
import com.tencent.devops.common.expression.expression.functions.NoOperation
import com.tencent.devops.common.expression.expression.sdk.Container
import com.tencent.devops.common.expression.expression.sdk.ExpressionNode
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.expression.expression.sdk.NoOperationNamedValue
import com.tencent.devops.common.expression.expression.sdk.operators.And
import com.tencent.devops.common.expression.expression.sdk.operators.Or
import com.tencent.devops.common.expression.expression.tokens.Associativity
import com.tencent.devops.common.expression.expression.tokens.Token
import com.tencent.devops.common.expression.expression.tokens.TokenKind
import com.tencent.devops.common.expression.expression.tokens.peek
import com.tencent.devops.common.expression.expression.tokens.pop
import com.tencent.devops.common.expression.expression.tokens.push
import java.util.regex.Pattern

@Suppress(
    "NestedBlockDepth",
    "ComplexCondition",
    "EmptyFunctionBlock",
    "TooManyFunctions",
    "LoopWithTooManyJumpStatements",
    "ReturnCount",
    "ThrowsCount",
    "MagicNumber"
)
object ExpressionParser {

    /**
     * 通过已有字典计算表达式
     * @param fetchValue 是否将计算结果还原为java类型
     */
    fun evaluateByContext(
        expression: String,
        context: ExecutionContext,
        nameValue: List<NamedValueInfo>,
        fetchValue: Boolean
    ): Any? {
        val result = createTree(expression.legalizeExpression(), null, nameValue, null)!!
            .evaluate(null, context, null, null)
        if (!fetchValue) {
            return result
        }
        return if (result.value is PipelineContextData) result.value.fetchValue() else result.value
    }

    /**
     * 通过Map变量表类型计算表达式
     * @param fetchValue 是否将计算结果还原为java类型
     */
    fun evaluateByMap(expression: String, contextMap: Map<String, String>, fetchValue: Boolean): Any? {
        val context = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()
        fillContextByMap(contextMap, context, nameValue)

        val result = createTree(expression.legalizeExpression(), null, nameValue, null)!!
            .evaluate(null, context, null, null)

        if (!fetchValue) {
            return result
        }

        return if (result.value is PipelineContextData) result.value.fetchValue() else result.value
    }

    fun fillContextByMap(
        contextMap: Map<String, String>,
        context: ExecutionContext,
        nameValue: MutableList<NamedValueInfo>
    ) {
        contextMap.forEach { (key, value) ->
            var data: DictionaryContextData? = null
            val tokens = key.split('.')
            if (tokens.size > 1) {
                tokens.forEachIndexed { index, token ->
                    if (index == tokens.size - 1) {
                        data!!.add(token, StringContextData(value))
                        return@forEachIndexed
                    }

                    if (index == 0) {
                        if (context.expressionValues[token] != null) {
                            data = context.expressionValues[token] as DictionaryContextData
                            return@forEachIndexed
                        }
                        nameValue.add(NamedValueInfo(token, ContextValueNode()))
                        context.expressionValues[token] = DictionaryContextData()
                        data = context.expressionValues[token] as DictionaryContextData
                        return@forEachIndexed
                    }

                    data!![token]?.let {
                        data = it as DictionaryContextData
                        return@forEachIndexed
                    }
                    data!![token] = DictionaryContextData()
                    data = data!![token] as DictionaryContextData
                }
            } else {
                nameValue.add(NamedValueInfo(key, ContextValueNode()))
                context.expressionValues[key] = StringContextData(value)
            }
        }
    }

    fun createTree(
        expression: String,
        trace: ITraceWriter?,
        namedValues: Iterable<INamedValueInfo>?,
        functions: Iterable<IFunctionInfo>?
    ): IExpressionNode? {
        val context = ParseContext(expression, trace, namedValues, functions)
        context.trace.info("Parsing expression: <$expression>")
        return createTreeByContext(context)
    }

    fun createSubNameValueEvaluateTree(
        expression: String,
        trace: ITraceWriter?,
        namedValues: Iterable<INamedValueInfo>?,
        functions: Iterable<IFunctionInfo>?,
        subNameValueEvaluateInfo: SubNameValueEvaluateInfo
    ): IExpressionNode? {
        val context = ParseContext(
            expression = expression,
            trace = trace,
            namedValues = namedValues,
            functions = functions,
            subNameValueEvaluateInfo = subNameValueEvaluateInfo
        )
        context.trace.info("SubNameValue Parsing expression: <$expression>")
        return createTreeByContext(context)
    }

    fun validateSyntax(
        expression: String,
        trace: ITraceWriter?
    ): IExpressionNode? {
        val context = ParseContext(expression, trace, null, null, true)
        context.trace.info("Validating expression syntax: <$expression>")
        return createTreeByContext(context)
    }

    private fun String.legalizeExpression(): String {
        val expression = this
        val regex = "jobs\\.([\\S]+)\\.([0-9]+)\\.steps\\.([\\S]+)\\.outputs\\.([\\S]+)"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(expression)
        return if (matcher.find()) {
            expression.replace(
                Regex(regex),
                "jobs.${matcher.group(1)}[${matcher.group(2)}].steps.${matcher.group(3)}.outputs.${matcher.group(4)}"
            )
        } else expression
    }

    private fun createTreeByContext(context: ParseContext): IExpressionNode? {
        // 推入 token
        while (true) {
            val (token, ok) = context.lexicalAnalyzer.tryGetNextToken()
            if (!ok) {
                break
            }
            context.token = token

            // Unexpected
            if (context.token?.kind == TokenKind.Unexpected) {
                throw ExpressionParseException(ParseExceptionKind.UnexpectedSymbol, context.token, context.expression)
            } else if (context.token?.isOperator == true) {
                pushOperator(context)
            } else {
                pushOperand(context)
            }

            context.lastToken = context.token
        }

        // No tokens
        if (context.lastToken == null) {
            return null
        }

        // Check unexpected end of expression
        if (context.operators.isNotEmpty()) {
            var unexpectedLastToken = false
            when (context.lastToken?.kind) {
                TokenKind.EndGroup, // ")" logical grouping
                TokenKind.EndIndex, // "]"
                TokenKind.EndParameters -> { // ")" function call
                    // Legal
                }

                TokenKind.Function -> {
                    // Illegal
                    unexpectedLastToken = true
                }

                else -> {
                    unexpectedLastToken = context.lastToken?.isOperator == true
                }
            }

            if (unexpectedLastToken || context.lexicalAnalyzer.unclosedTokens.any()) {
                throw ExpressionParseException(
                    ParseExceptionKind.UnexpectedEndOfExpression,
                    context.lastToken,
                    context.expression
                )
            }
        }

        // Flush operators
        while (context.operators.isNotEmpty()) {
            flushTopOperator(context)
        }

        // Check max depth
        val result = context.operands.single()
        checkMaxDepth(context, result)

        return result
    }

    private fun pushOperand(context: ParseContext) {
        // Create the node
        val node: ExpressionNode?
        when (context.token?.kind) {
            // Function
            TokenKind.Function -> {
                val function = context.token?.rawValue
                val (functionInfo, ok) = tryGetFunctionInfo(context, function)
                if (ok) {
                    node = functionInfo!!.createNode()
                    node.name = function ?: ""
                } else if (context.allowUnknownKeywords) {
                    node = NoOperation()
                    node.name = function ?: ""
                } else {
                    throw ExpressionParseException(
                        ParseExceptionKind.UnrecognizedFunction,
                        context.token,
                        context.expression
                    )
                }
            }

            // Named-value
            TokenKind.NamedValue -> {
                val name = context.token?.rawValue
                if (context.extensionNamedValues[name] != null) {
                    node = context.extensionNamedValues[name]?.createNode()
                    node?.name = name ?: ""
                } else if (context.allowUnknownKeywords) {
                    node = NoOperationNamedValue()
                    node.name = name ?: ""
                } else if (context.subNameValueEvaluateInfo?.enableSubNameValueEvaluate == true) {
                    node = ContextValueNode()
                    node.name = name ?: ""
                } else {
                    throw ExpressionParseException(
                        ParseExceptionKind.UnrecognizedNamedValue,
                        context.token,
                        context.expression
                    )
                }
            }

            // Otherwise simple
            else -> {
                node = context.token?.toNode()
            }
        }

        // Push the operand
        context.operands.push(node!!)
    }

    private fun pushOperator(context: ParseContext) {
        // 刷新更高或相等的优先级
        if (context.token!!.associativity == Associativity.LeftToRight) {
            val precedence = context.token!!.precedence
            while (context.operators.isNotEmpty()) {
                val topOperator = context.operators.peek()
                if (precedence <= topOperator.precedence &&
                    topOperator.kind != TokenKind.StartGroup && // Unless top is "(" logical grouping
                    topOperator.kind != TokenKind.StartIndex && // or unless top is "["
                    topOperator.kind != TokenKind.StartParameters && // or unless top is "(" function call
                    topOperator.kind != TokenKind.Separator
                ) {
                    // or unless top is ","
                    flushTopOperator(context)
                    continue
                }
                break
            }
        }

        // Push the operator
        context.operators.push(context.token!!)

        // 处理关闭操作符，因为 context.LastToken 是必需的
        // 为了准确处理 TokenKind.EndParameters
        when (context.token!!.kind) {
            TokenKind.EndGroup, // ")" logical grouping
            TokenKind.EndIndex, // "]"
            TokenKind.EndParameters -> // ")" function call
                flushTopOperator(context)

            else -> {}
        }
    }

    private fun flushTopOperator(context: ParseContext) {
        // Special handling for closing operators
        when (context.operators.peek().kind) {
            TokenKind.EndIndex -> { // "]"
                flushTopEndIndex(context)
                return
            }

            TokenKind.EndGroup -> { // ")" logical grouping
                flushTopEndGroup(context)
                return
            }

            TokenKind.EndParameters -> { // ")" function call
                flushTopEndParameters(context)
                return
            }

            else -> {}
        }

        // Pop the operator
        val operator = context.operators.pop()

        // Create the node
        val node = operator.toNode() as Container

        // Pop the operands, add to the node
        val operands = popOperands(context, operator.operandCount)
        operands.forEach { operand ->
            // Flatten nested And
            if (node is And) {
                if (operand is And) {
                    operand.parameters.forEach { nestedParameter ->
                        node.addParameters(nestedParameter)
                    }

                    return@forEach
                }
            }
            // Flatten nested Or
            else if (node is Or) {
                if (operand is Or) {
                    operand.parameters.forEach { nestedParameter ->
                        node.addParameters(nestedParameter)
                    }

                    return@forEach
                }
            }

            node.addParameters(operand)
        }

        // Push the node to the operand stack
        context.operands.push(node)
    }

    // / <summary>
    // / Flushes the ")" logical grouping operator
    // / </summary>
    private fun flushTopEndGroup(context: ParseContext) {
        // Pop the operators
        // ")" logical grouping
        popOperator(context, TokenKind.EndGroup)
        // "(" logical grouping
        popOperator(context, TokenKind.StartGroup)
    }

    // / <summary>
    // / Flushes the "]" operator
    // / </summary>
    private fun flushTopEndIndex(context: ParseContext) {
        // Pop the operators
        // "]"
        popOperator(context, TokenKind.EndIndex)
        val operator = popOperator(context, TokenKind.StartIndex) // "["

        // Create the node
        val node = operator.toNode() as Container

        // Pop the operands, add to the node
        val operands = popOperands(context, operator.operandCount)
        operands.forEach { operand ->
            node.addParameters(operand)
        }

        // Push the node to the operand stack
        context.operands.push(node)
    }

    // ")" function call
    private fun flushTopEndParameters(context: ParseContext) {
        // Pop the operator
        // ")" function call
        var operator = popOperator(context, TokenKind.EndParameters)

        // Sanity check top operator is the current token
        if (operator != context.token) {
            throw InvalidOperationException("Expected the operator to be the current token")
        }

        val function: Function?

        // No parameters
        if (context.lastToken?.kind == TokenKind.StartParameters) {
            // Node already exists on the operand stack
            function = context.operands.peek() as Function
        }
        // Has parameters
        else {
            // Pop the operands
            var parameterCount = 1
            while (context.operators.peek().kind == TokenKind.Separator) {
                parameterCount++
                context.operators.pop()
            }
            val functionOperands = popOperands(context, parameterCount)

            // Node already exists on the operand stack
            function = context.operands.peek() as Function

            // Add the operands to the node
            functionOperands.forEach { operand ->
                function.addParameters(operand)
            }
        }

        // Pop the "(" operator too
        operator = popOperator(context, TokenKind.StartParameters)

        // Check min/max parameter count
        val (functionInfo, ok) = tryGetFunctionInfo(context, function.name)
        if (!ok) {
            throw NotSupportedException("not get function ${function.name} info")
        }

        if (functionInfo == null && context.allowUnknownKeywords) {
            // Don't check min/max
        } else if (function.parameters.count() < functionInfo!!.minParameters) {
            throw ExpressionParseException(ParseExceptionKind.TooFewParameters, operator, context.expression)
        } else if (function.parameters.count() > functionInfo.maxParameters) {
            throw ExpressionParseException(ParseExceptionKind.TooManyParameters, operator, context.expression)
        }
    }

    // / <summary>
    // / Pops N operands from the operand stack. The operands are returned
    // / in their natural listed order, i.e. not last-in-first-out.
    // / </summary>
    private fun popOperands(
        context: ParseContext,
        c: Int
    ): List<ExpressionNode> {
        var count = c
        val result = mutableListOf<ExpressionNode>()
        while (count-- > 0) {
            result.add(context.operands.pop())
        }

        result.reverse()
        return result
    }

    // / <summary>
    // / Pops an operator and asserts it is the expected kind.
    // / </summary>
    private fun popOperator(
        context: ParseContext,
        expected: TokenKind
    ): Token {
        val token = context.operators.pop()
        if (token.kind != expected) {
            throw NotSupportedException("Expected operator '$expected' to be popped. Actual '${token.kind}'.")
        }
        return token
    }

    // / <summary>
    // / Checks the max depth of the expression tree
    // / </summary>
    private fun checkMaxDepth(
        context: ParseContext,
        node: ExpressionNode,
        depth: Int = 1
    ) {
        if (depth > ExpressionConstants.MAX_DEEP) {
            throw ExpressionParseException(ParseExceptionKind.ExceededMaxDepth, null, context.expression)
        }

        if (node is Container) {
            node.parameters.forEach { parameter ->
                checkMaxDepth(context, parameter, depth + 1)
            }
        }
    }

    private fun tryGetFunctionInfo(
        context: ParseContext,
        name: String?
    ): Pair<IFunctionInfo?, Boolean> {
        var result = ExpressionConstants.WELL_KNOWN_FUNCTIONS[name]
        if (result != null) {
            return Pair(result, true)
        }
        result = context.extensionFunctions[name]
        if (result != null) {
            return Pair(result, true)
        }

        return Pair(null, false)
    }
}
