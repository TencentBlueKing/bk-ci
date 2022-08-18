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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.expression.DistinguishType
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("ComplexMethod", "LongMethod", "MaxLineLength")
@DisplayName("Yaml解析模板工具综合测试")
internal class TemplateYamlUtilTest {

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
        val result = TemplateYamlUtil.fromJsonToArrayContext("", "lll", testData)
        val c = JsonUtil.getObjectMapper().writeValueAsString(expectDate.toJson())
        val a = JsonUtil.getObjectMapper().writeValueAsString(result.toJson())
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
    if: "join(parameters.arry[3]) == join(fromJSON('["12138"]'))"
    run: |
      echo ${'$'}{{parameters.bool == true}} == ${'$'}{{parameters.bool == variables.xxx}}
      echo \{{parameters.bool == true}} == ${'$'}{{variables.xxx}}
    """.trimIndent()

    private val parseParameterValueTestResult = """
version: v2.0

varaibles:
  VAR_A: "false"
  VAR_B: "12312.12"
  VAR_C: [12312.12,false,"12138",["12138"]]
  VAR_D: "12138"
  VAR_D: "false"
  VAR_D: "true"
  VAR_D: "true"
  VAR_D: "true"
  VAR_D: "${'$'}{{ (false == variables.xxx) }}"
  VAR_D: "true"
  VAR_D: "${'$'}{{ variables.xxx[123][xxx] }}"
  script: "cd src\nmkdir build\ncd build\ncmake -DLIB_LEGO_DIR=${'$'}{{ ci.workspace }}/tmp/liblego ..\nmake -j`nproc`"

steps:
  - name: xxx
    if: "false"
    if: "true"
    if: "(false == variables.xxx)"
    if: "(false == variables.xxx)"
    if: "(world == world)"
    if: "true"
    run: |
      echo false == ${'$'}{{ (false == variables.xxx) }}
      echo \{{parameters.bool == true}} == ${'$'}{{ variables.xxx }}

    """.trimIndent()

    @DisplayName("替换模板测试")
    @Test
    fun parseParameterValueTest() {
        val r = Runtime.getRuntime()
        r.gc() // 计算内存前先垃圾回收一次
        val start = System.currentTimeMillis() // 开始Time
        val startMem = r.freeMemory() // 开始Memory

        val res = TemplateYamlUtil.parseParameterValue(
            path = "", value = parseParameterValueTestData, nameValues = parametersNameValue,
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
            "2 => parameters.arry => '[12312.12,false,\"12138\",[\"12138\"]]'",
            "3 => fromJSON(parameters['arry']) == variables.arry => (fromJSON('[12312.12,false,\"12138\",[\"12138\"]]') == variables.arry)",
            "4 => fromJSON(parameters['arry'])[0] == variables.arry[0] => (fromJSON('[12312.12,false,\"12138\",[\"12138\"]]')[0] == variables.arry[0])",
            "5 => parameters.doub => 12312.12",
            "6 => parameters.bool => false",
            "7 => parameters.str => 12138",
            "8 => startsWith(parameters.str, '121') => true"
        ]
    )
    fun expressionEvaluateTest(expressionEvaluateStr: String) {
        val (index, exp, result) = expressionEvaluateStr.split("=>").map { it.trim() }
        var (res, _) = TemplateYamlUtil.expressionEvaluate(
            path = "",
            expression = exp,
            needBrackets = true,
            distinguishTypes = setOf(DistinguishType.ARRAY),
            nameValues = parametersNameValue,
            context = parametersEv
        )
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
            "\${{parameters.xxx == xxxx}} => [0,26]",
            " \${{parameters.xxx == xxxx}} => [1, 27]",
            "\${{parameters.xxx == xxxx}} => [0, 26]",
            " \${{parameters.xxx == xxxx}} => [1, 27]",
            " \${{parameters.xxx == xxxx}} => [1, 27]",
            " \${{parameters.xxx == xxxx => []",
            "parameters.xxx == xxxx }} => []",
            "parameters.xxx == xxxx => []",
            "aaa: xx == \${{ parameters.xxx == xxxx}} == \${{xxxx }} !\${xx}}=> [11, 38, 43, 52]"
        ]
    )
    fun findExpressionsTest(findCondition: String) {
        val (cond, result) = findCondition.split("=>")
        val list = TemplateYamlUtil.findExpressions(cond)
        Assertions.assertEquals(JsonUtil.to<List<Int>>(result.trim()), list)
    }

    companion object {
        val parametersNameValue = mutableListOf<NamedValueInfo>()
        val parametersEv = ExecutionContext(DictionaryContextData())

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
            }
            parametersEv.expressionValues.add("parameters", parametersData)
        }
    }
}
