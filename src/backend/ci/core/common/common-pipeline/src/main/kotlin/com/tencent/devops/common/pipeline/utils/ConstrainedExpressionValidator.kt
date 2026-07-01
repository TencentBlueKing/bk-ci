/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.expression.EvalExpress
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo

/**
 * 创作流制约风格下，双花括号表达式内容的合规性校验。
 *
 * 除直接以 [EvalExpress.contextPrefix] 前缀开头的上下文引用外，还支持
 * [ExpressionConstants.WELL_KNOWN_FUNCTIONS] 中注册的表达式函数（如 fromJSON、contains、join 等），
 * 且函数参数中的命名值根节点须在允许的上下文前缀集合内。
 *
 * 与矩阵动态策略 [MatrixYamlCheckUtils] 中 `${{ fromJSON(xxx) }}` 占位符语义对齐。
 */
object ConstrainedExpressionValidator {

    private val KNOWN_FUNCTIONS = ExpressionConstants.WELL_KNOWN_FUNCTIONS.values.toList()

    /**
     * 校验双花括号内的表达式是否合规。
     *
     * @param expression 花括号内的原始表达式（不含 `${{` / `}}`）
     * @param allowedPrefixes 允许的上下文前缀，默认与 [EvalExpress.contextPrefix] 一致
     */
    @JvmStatic
    fun isValidDoubleBraceExpression(
        expression: String,
        allowedPrefixes: List<String> = EvalExpress.contextPrefix
    ): Boolean {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        if (allowedPrefixes.any { trimmed.startsWith(it) }) {
            return true
        }
        return isParseableConstrainedExpression(trimmed, allowedPrefixes)
    }

    private fun isParseableConstrainedExpression(
        expression: String,
        allowedPrefixes: List<String>
    ): Boolean {
        val namedValues = allowedPrefixes
            .map { it.removeSuffix(".") }
            .distinct()
            .map { NamedValueInfo(it, ContextValueNode()) }
        return try {
            ExpressionParser.createTree(
                expression = expression,
                trace = null,
                namedValues = namedValues,
                functions = KNOWN_FUNCTIONS
            ) != null
        } catch (ignored: ExpressionParseException) {
            false
        } catch (ignored: Throwable) {
            false
        }
    }
}
