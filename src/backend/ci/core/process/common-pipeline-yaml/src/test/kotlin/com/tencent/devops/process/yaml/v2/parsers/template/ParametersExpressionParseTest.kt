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

package com.tencent.devops.process.yaml.v2.parsers.template

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.SubNameValueResultType
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.ExpressionContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.process.yaml.v2.parsers.template.models.ExpressionBlock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@Suppress("ComplexMethod", "LongMethod", "MaxLineLength")
@DisplayName("解析模板表达式综合测试")
internal class ParametersExpressionParseTest {

    @DisplayName("将列表类型模板参数通过JSON转为上下文测试")
    @Test
    fun fromJsonToArrayContextTest() {
        val testData = listOf(mapOf("toolList" to listOf("GOML")), mapOf("toolList" to listOf("CLOC", "DUPC", "CCN")))
        val expectDate = ArrayContextData().apply {
            add(
                DictionaryContextData().apply {
                    add("toolList", ArrayContextData().apply { add(StringContextData("GOML")) })
                }
            )
            add(
                DictionaryContextData().apply {
                    add(
                        "toolList",
                        ArrayContextData().apply {
                            add(StringContextData("CLOC"))
                            add(StringContextData("DUPC"))
                            add(StringContextData("CCN"))
                        }
                    )
                }
            )
        }
        val result = ParametersExpressionParse.fromJsonToArrayContext("", "lll", testData)
        Assertions.assertTrue(expectDate.toJson() == result.toJson())
    }

    private val parseParameterValueTestData = """
version: v2.0

varaibles:
  VAR_A: "${'$'}{{parameters.bool}}"
  VAR_B: "${'$'}{{parameters.doub}}"
  VAR_C: "${'$'}{{parameters.arry}}"
  VAR_D: "${'$'}{{parameters.str}}"
  VAR_D: "${'$'}{{ parameters.bool == true}}"
  VAR_D: "${'$'}{{ parameters.doub == 12312.12}}"
  VAR_D: "${'$'}{{ parameters.str == '12138'}}"
  VAR_D: "${'$'}{{ contains(parameters.arry, false)}}"
  VAR_D: "${'$'}{{ parameters.bool == variables.xxx}}"
  VAR_D: "${'$'}{{ parameters.dic['arry'][2] == '12138'}}"
  VAR_D: "${'$'}{{ variables.xxx[123][xxx] }}"
  script: "${'$'}{{ parameters.script }}"

steps:
  - name: xxx
    if: "${'$'}{{ parameters.bool == true}} == true"
    if: "parameters.bool == parameters.bool"
    if: "parameters.bool == variables.xxx"
    if: "parameters.bool == ${'$'}{{variables.xxx}}"
    if: "world == world"
    if: "join(parameters.arry[3]) == join(fromJSON('[\"12138\"]'))"
    run: |
      echo ${'$'}{{parameters.bool == true}} == ${'$'}{{parameters.bool == variables.xxx}}
      echo \{{parameters.bool == true}} == ${'$'}{{variables.xxx}}
      echo ${'$'}{{parameters.bool == parameters.${'$'}{{ parameters.bool }}}} == ${'$'}{{variables.${'$'}{{ parameters.bool }}.xxx}}
    """.trimIndent()

    private val parseParameterValueTestResult = """
version: v2.0

varaibles:
  VAR_A: "false"
  VAR_B: "12312.12"
  VAR_C: [12312.12,false,"12138",["12138"]]
  VAR_D: "12138"
  VAR_D: "${'$'}{{ (false == true) }}"
  VAR_D: "${'$'}{{ (12312.12 == 12312.12) }}"
  VAR_D: "${'$'}{{ ('12138' == '12138') }}"
  VAR_D: "${'$'}{{ contains('[12312.12,false,\"12138\",[\"12138\"]]', false) }}"
  VAR_D: "${'$'}{{ (false == variables.xxx) }}"
  VAR_D: "${'$'}{{ ('12138' == '12138') }}"
  VAR_D: "${'$'}{{ variables.xxx[123][xxx] }}"
  script: "cd src\nmkdir build\ncd build\ncmake -DLIB_LEGO_DIR=${'$'}{{ ci.workspace }}/tmp/liblego ..\nmake -j`nproc`"

steps:
  - name: xxx
    if: "((false == true) == true)"
    if: "(false == false)"
    if: "(false == variables.xxx)"
    if: "(false == variables.xxx)"
    if: "(world == world)"
    if: "(join('[\"12138\"]') == join(fromJSON('[\"12138\"]')))"
    run: |
      echo ${'$'}{{ (false == true) }} == ${'$'}{{ (false == variables.xxx) }}
      echo \{{parameters.bool == true}} == ${'$'}{{ variables.xxx }}
      echo ${'$'}{{ (false == '') }} == ${'$'}{{ variables.false.xxx }}

    """.trimIndent()

    @DisplayName("替换模板测试")
    @Test
    fun parseParameterValueTest() {
        val r = Runtime.getRuntime()
        r.gc() // 计算内存前先垃圾回收一次
        val start = System.currentTimeMillis() // 开始Time
        val startMem = r.freeMemory() // 开始Memory

        val res = ParametersExpressionParse.parseParameterValue(
            path = "",
            value = parseParameterValueTestData,
            nameValues = parametersNameValue,
            context = parametersEv
        )

        val endMem = r.freeMemory() // 末尾Memory
        val end = System.currentTimeMillis() // 末尾Time
        // 输出
        println("用时消耗: ${end - start}ms")
        println("内存消耗: ${(startMem - endMem) / 1024}KB")

        Assertions.assertEquals(parseParameterValueTestResult, res)
    }

    @DisplayName("解析表达式测试")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "1 => parameters.dic => {\"doub\":12312.12,\"bool\":false,\"str\":\"12138\",\"arry\":[12312.12,false,\"12138\",[\"12138\"]],\"dic\":{\"str\":\"12138\"}}",
            "2 => parameters.arry => [12312.12,false,\"12138\",[\"12138\"]]",
            "3 => fromJSON(parameters['arry']) == variables.arry => (fromJSON('[12312.12,false,\\\"12138\\\",[\\\"12138\\\"]]') == variables.arry)",
            "4 => fromJSON(parameters['arry'])[0] == variables.arry[0] => (fromJSON('[12312.12,false,\\\"12138\\\",[\\\"12138\\\"]]')[0] == variables.arry[0])",
            "5 => parameters.doub => 12312.12",
            "6 => parameters.bool => false",
            "7 => parameters.str => 12138",
            "8 => startsWith(parameters.str, '121') => \${{ startsWith('12138', '121') }}"
        ]
    )
    fun expressionEvaluateTest(expressionEvaluateStr: String) {
        val (index, exp, result) = expressionEvaluateStr.split("=>").map { it.trim() }
        var (res, isComplete, type) = ParametersExpressionParse.expressionEvaluate(
            path = "",
            expression = exp,
            needBrackets = true,
            nameValues = parametersNameValue,
            context = parametersEv
        )
        if (isComplete && (type == SubNameValueResultType.ARRAY || type == SubNameValueResultType.DICT)) {
            res = res.replace("\\\"", "\"")
        }
        if (index == "3" || index == "4") {
            Assertions.assertTrue(res.startsWith("\${{") && res.endsWith("}}"))
            res = res.removeSurrounding("\${{ ", " }}")
        }
        Assertions.assertEquals(result, res)
    }

    @DisplayName("寻找语句中的表达式测试")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "\${{parameters.xxx == xxxx}} => 0",
            " \${{parameters.xxx == xxxx}} => 1",
            "\${{parameters.xxx == xxxx}} => 2",
            " \${{parameters.xxx == xxxx}} => 3",
            " \${{parameters.xxx == xxxx}} => 4",
            " \${{parameters.xxx == xxxx => 5",
            "parameters.xxx == xxxx }} => 6",
            "parameters.xxx == xxxx => 7",
            "aaa: xx == \${{ parameters.xxx == xxxx}} == \${{xxxx }} !\${xx}}=> 8",
            "\${{ 4  \${{ 2 \${{ 1 }} }} \${{ 3 }} }} => 9",
            "\${{ 4  \${{ 2 \${{ 1 }} }} \${{ 3 }} => 10",
            "\${{ 4  2 \${{ 1 }} }} \${{ 3 }} => 11",
            "4  2 \${{ 1 }} }} 3 }} => 12"
        ]
    )
    fun findExpressionsTest(findCondition: String) {
        val (cond, index) = findCondition.split("=>")
        val list = ParametersExpressionParse.findExpressions(cond)
        val expects = listOf(
            listOf(listOf(ExpressionBlock(0, 26))),
            listOf(listOf(ExpressionBlock(1, 27))),
            listOf(listOf(ExpressionBlock(0, 26))),
            listOf(listOf(ExpressionBlock(1, 27))),
            listOf(listOf(ExpressionBlock(1, 27))),
            listOf(),
            listOf(),
            listOf(),
            listOf(listOf(ExpressionBlock(11, 38), ExpressionBlock(43, 52))),
            listOf(listOf(ExpressionBlock(13, 20)), listOf(ExpressionBlock(7, 23), ExpressionBlock(25, 32))),
            listOf(listOf(ExpressionBlock(13, 20)), listOf(ExpressionBlock(7, 23), ExpressionBlock(25, 32))),
            listOf(listOf(ExpressionBlock(9, 16)), listOf(ExpressionBlock(0, 19), ExpressionBlock(21, 28))),
            listOf(listOf(ExpressionBlock(5, 12)))
        )
        Assertions.assertEquals(expects[index.trim().toInt()], list)
    }

    @DisplayName("解析表达式")
    @ParameterizedTest
    @MethodSource("parseExpressionTestSource")
    fun parseExpressionTest(sample: String, expect: String) {
        val expIndexList = ParametersExpressionParse.findExpressions(sample)
        val isIf = sample.startsWith("if")
        val result = ParametersExpressionParse.parseExpression(
            line = sample.trim(),
            path = "",
            blocks = expIndexList,
            needBrackets = !isIf,
            nameValues = parametersNameValue,
            context = parametersEv
        )
        Assertions.assertEquals(expect.trim(), result)
    }

    companion object {
        val parametersNameValue = mutableListOf<NamedValueInfo>()
        val parametersEv = ExecutionContext(DictionaryContextData())

        @JvmStatic
        fun parseExpressionTestSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "VAR_A: \"\${{parameters.bool}}\"",
                    "VAR_A: \"false\""
                ),
                Arguments.of(
                    "VAR_A: \"\${{parameters.arry}}\"",
                    "VAR_A: [12312.12,false,\"12138\",[\"12138\"]]"
                ),
                Arguments.of(
                    "echo \${{parameters.bool == true}} == \${{parameters.bool == variables.xxx}}",
                    "echo \${{ (false == true) }} == \${{ (false == variables.xxx) }}"
                ),
                Arguments.of(
                    "echo \${{parameters.bool == parameters.\${{ parameters.bool }}}} == \${{variables.\${{ parameters.bool }}.xxx == true}}",
                    "echo \${{ (false == '') }} == \${{ (variables.false.xxx == true) }}"
                ),
                Arguments.of(
                    "if: \${{(\${{ parameters.var }} == true) && \${{ parameters.str }} != test}}",
                    "if: ((\${{varaibles.xxx == settings.xxx}} == true) && ('12138' != test))"
                ),
                Arguments.of(
                    "if: \${{( parameters.var == true) && parameters.str != test}}",
                    "if: ((\${{varaibles.xxx == settings.xxx}} == true) && ('12138' != test))"
                ),
                Arguments.of(
                    "echo: \${{( parameters.var == true) && parameters.str != 'test'}}",
                    "echo: \${{ ((\${{varaibles.xxx == settings.xxx}} == true) && ('12138' != 'test')) }}"
                ),
                Arguments.of(
                    "echo: \${{( parameters.dic.\${{ varaibles.xxx }} == parameters.arry[\${{ varaibles.xxx }}]) }}",
                    "echo: \${{ ('' == '') }}"
                )
            )
        }

        @BeforeAll
        @JvmStatic
        fun initData() {
            parametersNameValue.add(NamedValueInfo("parameters", ContextValueNode()))
            val parametersData = DictionaryContextData().apply {
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
                add(
                    "script",
                    StringContextData("cd src\nmkdir build\ncd build\ncmake -DLIB_LEGO_DIR=${'$'}{{ ci.workspace }}/tmp/liblego ..\nmake -j`nproc`")
                )
                add("var", ExpressionContextData("\${{varaibles.xxx == settings.xxx}}"))
            }
            parametersEv.expressionValues.add("parameters", parametersData)
        }
    }
}
