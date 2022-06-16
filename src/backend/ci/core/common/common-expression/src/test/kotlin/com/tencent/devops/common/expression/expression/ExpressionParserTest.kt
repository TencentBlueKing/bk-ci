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

package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.expression.pipeline.contextData.ArrayContextData
import com.tencent.devops.common.expression.pipeline.contextData.BooleanContextData
import com.tencent.devops.common.expression.pipeline.contextData.ContextValueNode
import com.tencent.devops.common.expression.pipeline.contextData.DictionaryContextData
import com.tencent.devops.common.expression.pipeline.contextData.StringContextData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.exp

@DisplayName("表达式解析类综合测试")
class ExpressionParserTest {

    companion object {
        val ex = ExpressionParser()
        val ev = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()

        @BeforeAll
        @JvmStatic
        fun initData() {
            // 初始化操作符测试数据
            nameValue.add(NamedValueInfo("opTest", ContextValueNode()))
            val oArrayContextData = ArrayContextData().apply {
                add(StringContextData("test/test"))
                add(ArrayContextData().apply { add(StringContextData("aaa_-*+")) })
            }
            val oDictContextData = DictionaryContextData().apply {
                add("dic", StringContextData("dic"))
                add("dd", DictionaryContextData().apply { add("ddd", StringContextData("dd")) })
            }
            val oContextData = DictionaryContextData().apply {
                add("array", oArrayContextData)
                add("dic", oDictContextData)
                add("bool", BooleanContextData(false))
                add("strBool", StringContextData("false"))
            }
            ev.expressionValues.add("opTest", oContextData)

            // 初始化函数测试数据
            nameValue.add(NamedValueInfo("funcTest", ContextValueNode()))
            val fArrayContextData = ArrayContextData().apply {
                add(StringContextData("push"))
                add(StringContextData("mr"))
                add(StringContextData("tag"))
            }
            val fDictContextData = DictionaryContextData().apply {
                add(
                    "scallions",
                    DictionaryContextData().apply {
                        add(
                            "colors",
                            ArrayContextData().apply {
                                add(StringContextData("green"))
                                add(StringContextData("white"))
                                add(StringContextData("red"))
                            }
                        )
                        add(
                            "ediblePortions",
                            ArrayContextData().apply {
                                add(StringContextData("roots"))
                                add(StringContextData("stalks"))
                            }
                        )
                    }
                )
                add(
                    "beets",
                    DictionaryContextData().apply {
                        add(
                            "colors",
                            ArrayContextData().apply {
                                add(StringContextData("purple"))
                                add(StringContextData("red"))
                                add(StringContextData("gold"))
                                add(StringContextData("pink"))
                            }
                        )
                        add(
                            "ediblePortions",
                            ArrayContextData().apply {
                                add(StringContextData("roots"))
                                add(StringContextData("stems"))
                            }
                        )
                    }
                )
            }
            val farrayA = ArrayContextData().apply {
                add(StringContextData("roots"))
                add(StringContextData("stalks"))
            }
            val fContextData = DictionaryContextData().apply {
                add("array", fArrayContextData)
                add("arrayA", farrayA)
                add("dict", fDictContextData)
            }
            ev.expressionValues.add("funcTest", fContextData)
        }
    }

    @DisplayName("测试解析文字")
    @Test
    fun literalsTest() {
        val literals = mapOf<String, Any?>(
            "null" to null,
            "false" to false,
            "711" to 711.0,
            "-9.2" to -9.2,
            "-2.99e-2" to -2.99e-2,
            "'It''s open source!'" to "It's open source!"
        )

        literals.forEach { (exp, v) ->
            val res = ex.createTree(exp, null, null, null)!!.evaluate(null, null, null).value
            Assertions.assertEquals(v, res)
        }
    }

    @DisplayName("测试操作符: ()")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "1 == (2 != false) => true",
            "1 == (2 == false) => false"
        ]
    )
    fun operatorGroupTest(group: String) {
        valuesTest(group)
    }

    @DisplayName("测试操作符: []")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "opTest.array[0] => test/test",
            "opTest.array[1][0] => aaa_-*+",
            "opTest.array[2] => null",
            "opTest.dic['dic'] => dic"
        ]
    )
    fun operatorIndexTest(index: String) {
        valuesTest(index)
    }

    @DisplayName("测试操作符: .")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "opTest.dic.dic => dic",
            "opTest.dic.dd.ddd => dd"
        ]
    )
    fun operatorDereferenceTest(Dereference: String) {
        valuesTest(Dereference)
    }

    @DisplayName("测试操作符: !")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "!opTest.bool => true",
            "!0 => true",
            "!'0' => false",
            "!null => true",
            "!'' => true"
        ]
    )
    fun operatorNotTest(not: String) {
        valuesTest(not)
    }

    @DisplayName("测试操作符: <,<=,>,>=")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "true < false => false",
            "false < true => true",
            "true <= true => true",
            "'0' < '1' => true",
            "'0' <= '0' => true",
            "'str' <= 'stt' => true",
            "'str' <= 'strt' => true",
            "'str' <= 'str' => true",
            "true > false => true",
            "false > true => false",
            "true >= true => true",
            "'0' > '1' => false",
            "'0' >= '0' => true",
            "'str' >= 'stt' => false",
            "'str' >= 'strt' => false",
            "'str' >= 'str' => true",
        ]
    )
    fun operatorLessAndGreaterAndThan(lagat: String) {
        valuesTest(lagat)
    }

    /**
     * 如果类型不匹配，强制转换类型为数字。使用这些转换将数据类型转换为数字：

     * 类型	结果
     * Null	0
     * 布尔值	true 返回 1
     * false 返回 0
     * 字符串	从任何合法 JSON 数字格式剖析，否则为 NaN。
     * 注：空字符串返回 0。
     * 数组	NaN
     * 对象	NaN
     * 一个 NaN 与另一个 NaN 的比较不会产生 true。

     * 在比较字符串时不忽略大小写。

     * 对象和数组仅在为同一实例时才视为相等。
     */

    @DisplayName("测试操作符: ==")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "null == 0 => true",
            "true == 1 => true",
            "false == 0 => true",
            "'1' == 1 => true",
            "'' == 0 => true",
            "opTest.array == NaN => false",
            "opTest == NaN => false",
            "null == null => true",
            "true == true => true",
            "'str' == 'str' => true",
            "1.0 == 1 => true",
            "opTest.array[0] == 'test/test' => true",
            "opTest.array == opTest.array => true",
            "opTest == opTest => true"
        ]
    )
    fun operatorEqualTest(equal: String) {
        valuesTest(equal)
    }

    @DisplayName("测试操作符: !=")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "null != 0 => false",
            "true != 1 => false",
            "false != 0 => false",
            "'1' != 1 => false",
            "'' != 0 => false",
            "opTest.array != NaN => true",
            "opTest != NaN => true",
            "null != null => false",
            "true != true => false",
            "'str' != 'str' => false",
            "'Str' != 'Str' => false",
            "1.0 != 1 => false",
            "opTest.array != opTest.array => false",
            "opTest != opTest => false"
        ]
    )
    fun operatorNotEqualTest(notEqual: String) {
        valuesTest(notEqual)
    }

    @DisplayName("测试操作符: &&.||")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "true || false => true",
            "true && false => false",
            "(true == false) || (false == false) => true",
            "(true != false) && (false != false) => false"
        ]
    )
    fun operatorAndOrTest(Aor: String) {
        valuesTest(Aor)
    }

    @DisplayName("测试函数: contains(search, item)")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "contains('aaa', 'a') => true",
            "contains('aT', 'A') => false",
            "contains(12, 1) => true",
            "contains(12.0, 0) => false",
            "contains(true, 'tr') => true",
            "contains(funcTest.array, 'push') => true",
            "contains(funcTest.array, 'MR') => false",
            "contains(funcTest.array, 'MR') => false",
            "contains(funcTest.dict.scallions.ediblePortions, 'roots') => true",
            "contains(funcTest.dict.*.ediblePortions, funcTest.dict.scallions.ediblePortions) => true",
            "contains(funcTest.dict.*.ediblePortions, 'roots') => false",
            "contains(funcTest.dict.*.ediblePortions.*, 'roots') => true",
            "contains(funcTest.dict.*.ediblePortions.*, funcTest.arrayA.*) => false",
            "contains(!contains(funcTest.dict.*.ediblePortions.*, funcTest.arrayA.*), 'tr') => true"
        ]
    )
    fun functionContainsTest(contains: String) {
        valuesTest(contains)
    }

    @DisplayName("测试函数: startsWith(str, str)， endsWith(str, str)")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "startsWith('aaa', 'a') => true",
            "startsWith('aT', 'A') => false",
            "!startsWith('startsWith', 'st') => false",
            "startsWith(12, 1) => true",
            "startsWith(true, 'tr') => true",
            "startsWith(funcTest.array[1], 'm') => true",
            "startsWith(!contains(funcTest.dict.*.ediblePortions.*, funcTest.arrayA.*), 'tr') => true",
            "endsWith('aaa', 'a') => true",
            "endsWith('aT', 't') => false",
            "!endsWith('endsWith', 'th') => false",
            "endsWith(12, 2) => true",
            "endsWith(true, 'ue') => true",
            "endsWith(funcTest.array[1], 'r') => true",
            "endsWith(!contains(funcTest.dict.*.ediblePortions.*, funcTest.arrayA.*), 'ue') => true"
        ]
    )
    fun functionStartEndWithTest(startAndEnd: String) {
        valuesTest(startAndEnd)
    }

    private fun valuesTest(param: String) {
        val (exp, result) = param.split(" => ")
        val res = ex.createTree(exp, null, nameValue, null)!!.evaluate(null, Companion.ev, null).value
        Assertions.assertEquals(
            when (result) {
                "true", "false" -> {
                    result.toBoolean()
                }
                "null" -> {
                    null
                }
                else -> {
                    result
                }
            },
            res
        )
    }
}
