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

package com.tencent.devops.process.yaml.utils

import org.slf4j.LoggerFactory

/**
 * 插件参数联动(rely)表达式求值器。
 *
 * 插件 task.json 中通过 input.<param>.rely 描述参数显隐联动，形如：
 * rely = { expression: [{ key, value, regex }], operation: 'AND' | 'OR' | 'NOT' }
 *
 * 求值语义与前端保持一致（devops-pipeline/src/utils/util.js 的 rely 方法），
 * 保证后端清理出的结果与前端表单展示一致。求值异常时一律返回 true（即视为展示、不做清理），
 * 避免因配置异常导致用户参数被误删。
 */
object RelyExpressionEvaluator {

    private val logger = LoggerFactory.getLogger(RelyExpressionEvaluator::class.java)

    private const val OPERATION_AND = "AND"
    private const val OPERATION_OR = "OR"
    private const val OPERATION_NOT = "NOT"
    private const val KEY_EXPRESSION = "expression"
    private const val KEY_OPERATION = "operation"
    private const val KEY_KEY = "key"
    private const val KEY_VALUE = "value"
    private const val KEY_REGEX = "regex"

    /**
     * 判断参数当前是否应当展示。
     * @param rely 参数的 rely 配置
     * @param values 插件表单全量值（默认值与用户输入合并后的结果）
     * @return true 表示展示，false 表示因联动被隐藏
     */
    fun satisfy(rely: Any?, values: Map<String, Any?>): Boolean {
        return try {
            val relyMap = rely as? Map<*, *> ?: return true
            @Suppress("UNCHECKED_CAST")
            val expression = relyMap[KEY_EXPRESSION] as? List<Map<String, Any?>> ?: emptyList()
            val operation = relyMap[KEY_OPERATION]?.toString() ?: OPERATION_AND
            when (operation) {
                OPERATION_AND -> expression.all { match(it, values) }
                OPERATION_OR -> if (expression.isEmpty()) true else expression.any { match(it, values) }
                OPERATION_NOT -> if (expression.isEmpty()) true else !expression.any { match(it, values) }
                else -> true
            }
        } catch (ignored: Throwable) {
            logger.warn("evaluate atom param rely expression error, skip it", ignored)
            true
        }
    }

    private fun match(item: Map<String, Any?>, values: Map<String, Any?>): Boolean {
        val key = item[KEY_KEY]?.toString() ?: return false
        val expect = item[KEY_VALUE]
        val actual = values[key]
        // 配置的期望值为数组：表单值是数组时判是否有交集，否则判是否被包含。
        if (expect is Collection<*>) {
            return if (actual is Collection<*>) {
                expect.any { e -> actual.any { a -> valueEquals(e, a) } }
            } else if (actual == null) {
                false
            } else {
                expect.any { valueEquals(it, actual) }
            }
        }
        // 配置了正则：忽略大小写匹配
        val regex = item[KEY_REGEX]?.toString()
        if (!regex.isNullOrBlank()) {
            val reg = regex.toRegex(RegexOption.IGNORE_CASE)
            return if (actual is Collection<*>) {
                actual.any { it != null && reg.containsMatchIn(it.toString()) }
            } else {
                actual != null && reg.containsMatchIn(actual.toString())
            }
        }
        return if (actual is Collection<*>) {
            actual.any { valueEquals(it, expect) }
        } else {
            valueEquals(actual, expect)
        }
    }

    /**
     * 值比较：类型一致时直接比较，类型不一致（如数字与字符串）时退化为字符串比较，
     * 避免因序列化差异导致误判为不展示
     */
    private fun valueEquals(actual: Any?, expect: Any?): Boolean {
        if (actual == null || expect == null) return false
        return actual == expect || actual.toString() == expect.toString()
    }
}
