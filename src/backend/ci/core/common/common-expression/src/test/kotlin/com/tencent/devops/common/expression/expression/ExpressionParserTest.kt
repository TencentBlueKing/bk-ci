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
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.SubNameValueResultType
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.expression.utils.ExpressionJsonUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("ComplexMethod", "LongMethod", "MaxLineLength")
@DisplayName("表达式解析类综合测试")
class ExpressionParserTest {

    @DisplayName("测试流水线变量转换")
    @Test
    fun variablesConvert() {
        val variables = mapOf(
            "variables.pipeline_name" to "流水线名称",
            "variables.pipeline_id" to "p-xxx",
            "ci.actor" to "royalhuang",
            "envs.aaa" to "bbb",
            "jobs.job_1.steps.step_1.outputs.key_1" to "value1",
            "jobs.job_1.steps.step_2.outputs.key_1" to "value1",
            "jobs.job_2.steps.step_1.outputs.key_1" to "value1",
            "jobs.job_2.steps.step_1.outputs.key_2" to "value2",
            "jobs.build.0.steps.runStep.outputs.myoutput" to "build_0",
            "jobs.build.2.steps.runStep.outputs.myoutput" to "build_1",
            "depends.job1.outputs.matrix_include" to
                """[{"service":"api","var1":"b","var3":"yyy"},{"service":"c","cpu":"zzz"}]""",
            "project.name.chinese" to "蓝盾项目"
        )
        val expected = listOf(
            mapOf(
                "service" to "api",
                "var1" to "b",
                "var3" to "yyy"
            ),
            mapOf(
                "service" to "c",
                "cpu" to "zzz"
            )
        )
        Assertions.assertEquals(
            expected,
            ExpressionParser.evaluateByMap("fromJSON(depends.job1.outputs.matrix_include)", variables, true)
        )
        Assertions.assertEquals(
            true,
            ExpressionParser.evaluateByMap("jobs.build[0].steps.runStep.outputs.myoutput == 'build_0'", variables, true)
        )
        Assertions.assertEquals(
            true,
            ExpressionParser.evaluateByMap("jobs.build.0.steps.runStep.outputs.myoutput == 'build_0'", variables, true)
        )
        Assertions.assertEquals(
            true,
            ExpressionParser.evaluateByMap(
                "contains(jobs.build.0.steps.runStep.outputs.myoutput, 'build_0')", variables, true
            )
        )
        Assertions.assertEquals(
            true,
            ExpressionParser.evaluateByMap("variables.pipeline_name == '流水线名称'", variables, true)
        )
        Assertions.assertEquals(
            "p-xxx",
            ExpressionParser.evaluateByMap("variables.pipeline_id", variables, true)
        )
        assertThrows<ExpressionParseException> {
            ExpressionParser.evaluateByMap("a==a", variables, true)
        }
        println(ExpressionParser.evaluateByMap("variables.pipeline_id2", variables, true) ?: "variables.pipeline_id2")
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
            val res = ExpressionParser.createTree(exp, null, null, null)!!.evaluate(null, null, null, null).value
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
    fun operatorDereferenceTest(dereference: String) {
        valuesTest(dereference)
    }

    @DisplayName("测试操作符: !")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "!opTest.bool => true",
            "!0 => true",
            "!'0' => false",
            "!null => true",
            "!'' => true",
            "!opTest.strBool => true"
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
            "'str' >= 'str' => true"
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
            "false == '' => true",
            "null == 0 => true",
            "true == 1 => true",
            "false == 0 => true",
            "'1' == 1 => true",
            "'' == 0 => true",
            "opTest.array == NaN => false",
            "opTest == NaN => false",
            "null == null => true",
            "'false' == 0 => false",
            "'true' == true => true",
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
    fun operatorAndOrTest(aor: String) {
        valuesTest(aor)
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

    @DisplayName("测试函数: fromJson(str)")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "1 => fromJson('{\"include\":[{\"project\":\"foo\",\"config\":\"Debug\"},{\"project\":\"bar\"," +
                "\"config\":\"Release\"}]}')",
            "2 => fromJson(funcTest.json.strJson)",
            "3 => fromJson(funcTest.json.boolJson)",
            "4 => fromJson(funcTest.json.numJson)"
        ]
    )
    fun functionFromJsonTest(fromJson: String) {
        val (index, exp) = fromJson.split(" => ")
        val res = ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, null, null).value
        when (index.toInt()) {
            1 -> {
                Assertions.assertTrue(res is DictionaryContextData)
                val t1 = (res as DictionaryContextData)["include"]
                Assertions.assertTrue(t1 is ArrayContextData)
                val t2 = (t1 as ArrayContextData)[1]
                Assertions.assertTrue(t2 is DictionaryContextData)
                Assertions.assertEquals(
                    "Release", ((t2 as DictionaryContextData)["config"] as StringContextData).value
                )
            }

            2 -> {
                Assertions.assertTrue(res is ArrayContextData)
                Assertions.assertEquals("manager", ((res as ArrayContextData)[0] as StringContextData).value)
            }

            3 -> {
                Assertions.assertEquals(true, res)
            }

            4 -> {
                Assertions.assertEquals(1.0, res)
            }
        }
    }

    @DisplayName("测试函数: join(arr, selector)")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "1 => join(funcTest.array, '|')",
            "2 => join(funcTest.array)",
            "3 => join(funcTest.json.strJson)",
            "4 => join('123')",
            "5 => join(fromJson(funcTest.json.strJson))"
        ]
    )
    fun functionJoinTest(join: String) {
        val (index, exp) = join.split(" => ")
        val res = ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, null, null).value
        when (index.toInt()) {
            1 -> {
                Assertions.assertEquals("push|mr|tag", res)
            }

            2 -> {
                Assertions.assertEquals("push,mr,tag", res)
            }

            3 -> {
                Assertions.assertEquals("[\"manager\", \"webhook\"]", res)
            }

            4 -> {
                Assertions.assertEquals("123", res)
            }

            5 -> {
                Assertions.assertEquals("manager,webhook", res)
            }
        }
    }

    @DisplayName("部分参数替换测试(不含区分)")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "parameters => {\"int\":123,\"doub\":12312.12,\"bool\":false,\"str\":\"12138\",\"arry\":[12312.12,false,\"12138\",[\"12138\"]],\"dic\":{\"doub\":12312.12,\"bool\":false,\"str\":\"12138\",\"arry\":[12312.12,false,\"12138\",[\"12138\"]],\"dic\":{\"str\":\"12138\"}}}",
            "parameters.arry => [12312.12,false,\"12138\",[\"12138\"]]",
            "fromJSON(parameters['arry']) == variables.arry => (fromJSON('[12312.12,false,\\\"12138\\\",[\\\"12138\\\"]]') == variables.arry) => false",
            "fromJSON(parameters['arry'])[0] == variables.arry[0] => (fromJSON('[12312.12,false,\\\"12138\\\",[\\\"12138\\\"]]')[0] == variables.arry[0]) => true",
            "parameters.doub => 12312.12",
            "parameters.int => 123",
            "parameters.bool => false",
            "parameters.str => 12138",
            "startsWith(parameters.str, '121') => startsWith('12138', '121')",
            "'123' == test => ('123' == test)",
            "join(fromJSON('[\"12138\"]')) => join(fromJSON('[\"12138\"]'))"
        ]
    )
    fun subNameValueEvaluateTestDistinguishString(subDistinguish: String) {
        val items = subDistinguish.split(" => ")
        val exp = items[0]
        val result = items[1]
        val subInfo = SubNameValueEvaluateInfo()
        val tree = ExpressionParser.createSubNameValueEvaluateTree(exp, null, parametersNameValue, null, subInfo)!!
        var (res, isComplete, type) = tree.subNameValueEvaluate(null, parametersEv, null, subInfo, null)
        if (isComplete && (type == SubNameValueResultType.ARRAY || type == SubNameValueResultType.DICT)) {
            res = res.replace("\\\"", "\"")
        }
        Assertions.assertEquals(result, res)

        // yaml解析出的结果默认会带"" 这里模拟的是从yaml转为字符串再计算
        if (items.size == 3) {
            val trans = ExpressionJsonUtil.getObjectMapper().readValue("\"$res\"", String::class.java)
            valuesTest(trans + " => " + items[2])
        }
    }

    private fun valuesTest(param: String) {
        val (exp, result) = param.split(" => ")
        val res = ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, null, null).value
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

    companion object {
        val ev = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()
        val parametersNameValue = mutableListOf<NamedValueInfo>()
        val parametersEv = ExecutionContext(DictionaryContextData())

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
            val jsonTest = DictionaryContextData().apply {
                add("strJson", StringContextData("[\"manager\", \"webhook\"]"))
                add("boolJson", StringContextData("true"))
                add("numJson", StringContextData("1"))
            }
            val fContextData = DictionaryContextData().apply {
                add("array", fArrayContextData)
                add("arrayA", farrayA)
                add("dict", fDictContextData)
                add("json", jsonTest)
            }
            ev.expressionValues.add("funcTest", fContextData)

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
                        add(NumberContextData(12312.12))
                        add(BooleanContextData(false))
                        add(StringContextData("12138"))
                        add(
                            ArrayContextData().apply {
                                add(StringContextData("12138"))
                            }
                        )
                    }
                )
                add(
                    "dic",
                    DictionaryContextData().apply {
                        add("doub", NumberContextData(12312.12))
                        add("bool", BooleanContextData(false))
                        add("str", StringContextData("12138"))
                        add(
                            "arry",
                            ArrayContextData().apply {
                                add(NumberContextData(12312.12))
                                add(BooleanContextData(false))
                                add(StringContextData("12138"))
                                add(
                                    ArrayContextData().apply {
                                        add(StringContextData("12138"))
                                    }
                                )
                            }
                        )
                        add(
                            "dic",
                            DictionaryContextData().apply {
                                add("str", StringContextData("12138"))
                            }
                        )
                    }
                )
            }
            parametersEv.expressionValues.add("parameters", parametersData)
            nameValue.add(NamedValueInfo("variables", ContextValueNode()))
            ev.expressionValues.add("variables", parametersData)
        }
    }
}
