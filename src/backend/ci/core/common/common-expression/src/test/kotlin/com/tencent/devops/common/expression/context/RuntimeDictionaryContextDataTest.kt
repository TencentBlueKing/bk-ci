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

package com.tencent.devops.common.expression.context

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("ComplexMethod", "LongMethod", "MaxLineLength")
@DisplayName("运行时字典上下文综合测试")
internal class RuntimeDictionaryContextDataTest {

    @DisplayName("上下文单独测试")
    @Test
    fun contextSingleBuildTest() {
        val context = RuntimeDictionaryContextData(RuntimeNamedValueImpl())
        data.forEach { (k, v) ->
            Assertions.assertEquals(v, context.tryGetValue(k).first)
        }
        Assertions.assertEquals(data.map { it.value }, context.values)
    }

    @DisplayName("上下文参与表达式计算测试")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "settings.password.password => 123",
            "settings.access_token.access_token == '456' => true",
            "settings['appId'].secretKey == '101112' => true",
            "settings.token['username'] == " +
                "fromJSON('{\"token\":\"212223\",\"username\":\"789\",\"password\":\"123\"}').username => true"
        ]
    )
    fun contextExpressionParseTest(expAndExpect: String) {
        val (exp, expect) = expAndExpect.split(" => ")
        val res = ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, null, null).value
        Assertions.assertEquals(
            if (expect == "true" || expect == "false") {
                expect.toBoolean()
            } else {
                expect
            },
            res
        )
    }

    companion object {
        val ev = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()

        @BeforeAll
        @JvmStatic
        fun initData() {
            val runtimeNamedValue = RuntimeNamedValueImpl()
            nameValue.add(NamedValueInfo(runtimeNamedValue.key, ContextValueNode()))
            ev.expressionValues.add(
                runtimeNamedValue.key,
                RuntimeDictionaryContextData(runtimeNamedValue)
            )
        }

        private val data = mapOf(
            "password" to DictionaryContextData().apply {
                add("password", StringContextData("123"))
            },
            "access_token" to DictionaryContextData().apply {
                add("access_token", StringContextData("456"))
            },
            "username" to DictionaryContextData().apply {
                add("username", StringContextData("789"))
                add("password", StringContextData("123"))
            },
            "secretKey" to DictionaryContextData().apply {
                add("secretKey", StringContextData("101112"))
            },
            "appId" to DictionaryContextData().apply {
                add("appId", StringContextData("131415"))
                add("secretKey", StringContextData("101112"))
            },
            "privateKey" to DictionaryContextData().apply {
                add("privateKey", StringContextData("151617"))
                add("passphrase", StringContextData("181920"))
            },
            "token" to DictionaryContextData().apply {
                add("token", StringContextData("212223"))
                add("username", StringContextData("789"))
                add("password", StringContextData("123"))
            }
        )

        class RuntimeNamedValueImpl(override val key: String = "settings") : RuntimeNamedValue {
            override fun getValue(key: String) = data[key]
        }
    }
}
