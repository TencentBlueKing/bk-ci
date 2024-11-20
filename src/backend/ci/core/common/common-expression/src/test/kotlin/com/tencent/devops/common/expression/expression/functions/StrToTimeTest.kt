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

package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.FunctionFormatException
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.EvaluationOptions
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("ComplexMethod", "LongMethod", "MaxLineLength")
@DisplayName("测试strToTime函数")
internal class StrToTimeTest {

    @DisplayName("strToTime异常相关测试")
    @Test
    fun strToTimeExpcetionTest() {
        Assertions.assertThrows(FunctionFormatException::class.java) {
            ExpressionParser.createTree(
                "strToTime('2023-3-15')",
                null, nameValue, null
            )!!.evaluate(null, ev, EvaluationOptions(false), null)
        }
    }

    @DisplayName("evaluateCore相关测试")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "strToTime('2023-03-15') => strToTime('2023-03-15 00:00:00')"
        ]
    )
    fun evaluateCoreTest(format: String) {
        val (exp, expect) = format.split(" => ")
        val res1 = ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, EvaluationOptions(false), null).value
        val res2 = ExpressionParser.createTree(expect, null, nameValue, null)!!.evaluate(null, ev, EvaluationOptions(false), null).value
        Assertions.assertEquals(res1, res2)
    }

    @DisplayName("evaluateCore相关测试")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "strToTime('2023-03-15 12:06:22') > strToTime('2023-03-15 12:06:21') => true",
            "strToTime('2023-03-15 12:06:22') > strToTime('2023-03-16 12:06:21') => false"
        ]
    )
    fun eqTest(format: String) {
        val (exp, expect) = format.split(" => ")
        val res = ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, EvaluationOptions(false), null).value
        Assertions.assertEquals(expect, res.toString())
    }

    @DisplayName("subNameEvaluate相关测试")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "strToTime(parameters.date) => strToTime('2023-03-15')"
        ]
    )
    fun subNameEvaluateCoreTest(subNameFormat: String) {
        val (exp, expect) = subNameFormat.split(" => ")
        val res =
            ExpressionParser
                .createSubNameValueEvaluateTree(exp, null, parametersNameValue, null, SubNameValueEvaluateInfo())!!
                .subNameValueEvaluate(null, parametersEv, EvaluationOptions(false), SubNameValueEvaluateInfo(), null).value
        Assertions.assertEquals(expect, res)
    }

    companion object {
        val ev = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()
        val parametersNameValue = mutableListOf<NamedValueInfo>()
        val parametersEv = ExecutionContext(DictionaryContextData())

        @BeforeAll
        @JvmStatic
        fun initData() {
            nameValue.add(NamedValueInfo("variables", ContextValueNode()))
            val varData = DictionaryContextData().apply {
                add("int", NumberContextData(123.0))
                add("doub", NumberContextData(12312.12))
                add("bool", BooleanContextData(false))
                add("str", StringContextData("12138"))
                add(
                    "arry",
                    ArrayContextData().apply {
                        add(StringContextData("12138"))
                        add(StringContextData("111"))
                    }
                )
            }
            ev.expressionValues.add("variables", varData)

            // 初始化部分参数替换测试数据
            parametersNameValue.add(NamedValueInfo("parameters", ContextValueNode()))
            val parametersData = DictionaryContextData().apply {
                add("int", NumberContextData(123.0))
                add("doub", NumberContextData(12312.12))
                add("bool", BooleanContextData(false))
                add("str", StringContextData("12138"))
                add(
                    "arry",
                    ArrayContextData().apply {
                        add(StringContextData("12138"))
                    }
                )
                add("var", StringContextData("variables.xxx"))
                add("date", StringContextData("2023-03-15"))
            }
            parametersEv.expressionValues.add("parameters", parametersData)
        }
    }
}
