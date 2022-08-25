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

package com.tencent.devops.common.api.expression

import org.slf4j.LoggerFactory

@Suppress("ALL")
object EvalExpress {
    private val logger = LoggerFactory.getLogger(EvalExpress::class.java)
    private val contextPrefix = listOf("variables.", "settings", "envs.", "ci.", "job.", "jobs.", "steps.", "matrix.")

    fun eval(
        buildId: String,
        condition: String,
        variables: Map<String, Any>
    ): Boolean {
        logger.info("Enter eval condition: $condition")
        // 去掉花括号
        val baldExpress = condition.replace("\${{", "").replace("}}", "")
        logger.info("Condition without double curly: $baldExpress")

        val originItems: List<Word>

        // 先语法分析
        try {
            originItems = Lex(baldExpress.toList().toMutableList()).getToken()
            GrammarAnalysis(originItems).analysis()
        } catch (e: Exception) {
            logger.info(
                "[$buildId]|STAGE_CONDITION|skip|CUSTOM_CONDITION_MATCH|expression=$baldExpress|" +
                    "reason=Grammar Invalid: ${e.message}"
            )
            throw ExpressionException("expression=$baldExpress|reason=Grammar Invalid: ${e.message}")
        }

        // 替换变量
        val items = mutableListOf<Word>()
        originItems.forEach {
            if (it.symbol == "ident") {
                items.add(Word(replaceVariable(it.str, variables), it.symbol))
            } else {
                items.add(Word(it.str, it.symbol))
            }
        }
        val itemsStr = items.joinToString(" ") { it.str }
        logger.info("Condition after replace var: $itemsStr")
        // 再分析一次语法
        try {
            GrammarAnalysis(items).analysis()
        } catch (e: Exception) {
            logger.info(
                "[$buildId]|STAGE_CONDITION|skip|CUSTOM_CONDITION_MATCH|expression=$itemsStr|" +
                    "reason=Grammar Invalid: ${e.message}"
            )
            throw ExpressionException("parsed expression=$itemsStr|reason=Grammar Invalid: ${e.message}")
        }

        // 分析语义，计算表达式
        return try {
            SemanticAnalysis(items).analysis()
        } catch (e: Throwable) {
            logger.info(
                "[$buildId]|STAGE_CONDITION|skip|CUSTOM_CONDITION_MATCH|expression=$itemsStr|" +
                    "reason=Semantic analysis failed: ${e.message}"
            )
            throw ExpressionException("Eval expression=$itemsStr|reason=Semantic analysis failed: ${e.message}")
        }
    }

    private fun replaceVariable(str: String, variables: Map<String, Any>): String {
        // 暂时判断这些前缀，根据需要再加
        contextPrefix.forEach {
            if (str.startsWith(it)) {
                return variables[str] as String? ?: str
            }
        }

        return str
    }
}
