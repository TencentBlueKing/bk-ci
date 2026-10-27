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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RelyExpressionEvaluatorTest {

    @Test
    fun `rely config is empty should be shown`() {
        assertTrue(RelyExpressionEvaluator.satisfy(null, mapOf("p1" to "A")))
        assertTrue(RelyExpressionEvaluator.satisfy(emptyMap<String, Any>(), mapOf("p1" to "A")))
    }

    @Test
    fun `and operation`() {
        val rely = rely("AND", item("p1", "A"))
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "A")))
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "B")))
        // 控制参数缺失时视为不满足
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p2" to "A")))
    }

    @Test
    fun `multi expression with and operation`() {
        val rely = rely("AND", item("p1", "A"), item("p2", "B"))
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "A", "p2" to "B")))
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "A", "p2" to "C")))
    }

    @Test
    fun `or operation`() {
        val rely = rely("OR", item("p1", "A"), item("p1", "B"))
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "B")))
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "C")))
        // 空表达式默认展示
        assertTrue(RelyExpressionEvaluator.satisfy(rely("OR"), mapOf("p1" to "C")))
    }

    @Test
    fun `not operation`() {
        val rely = rely("NOT", item("p1", "A"))
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "B")))
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "A")))
        assertTrue(RelyExpressionEvaluator.satisfy(rely("NOT"), mapOf("p1" to "A")))
    }

    @Test
    fun `value is collection`() {
        val rely = rely("AND", item("p1", listOf("A", "B")))
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "B")))
        // 表单值为数组时判交集
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to listOf("C", "A"))))
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "C")))
    }

    @Test
    fun `regex match should ignore case`() {
        val rely = rely("AND", mapOf("key" to "p1", "regex" to "abc"))
        assertTrue(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "xxABCxx")))
        assertFalse(RelyExpressionEvaluator.satisfy(rely, mapOf("p1" to "xxExx")))
    }

    @Test
    fun `invalid config should be shown`() {
        assertTrue(RelyExpressionEvaluator.satisfy("invalid config", mapOf("p1" to "A")))
        assertTrue(
            RelyExpressionEvaluator.satisfy(
                mapOf("expression" to mapOf("key" to "p1"), "operation" to "AND"),
                mapOf("p1" to "A")
            )
        )
    }

    private fun item(key: String, value: Any?) = mapOf("key" to key, "value" to value)

    private fun rely(operation: String, vararg expression: Map<String, Any?>): Map<String, Any> = mapOf(
        "expression" to expression.toList(),
        "operation" to operation
    )
}
