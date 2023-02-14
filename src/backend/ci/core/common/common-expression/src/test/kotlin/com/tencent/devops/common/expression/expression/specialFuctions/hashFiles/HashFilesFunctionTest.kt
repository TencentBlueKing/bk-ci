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

package com.tencent.devops.common.expression.expression.specialFuctions.hashFiles

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.FunctionInfo
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.ClassPathResource

@Suppress("ALL")
@DisabledOnOs(OS.WINDOWS)
internal class HashFilesFunctionTest {

    @DisplayName("HashFiles相关异常测试")
    @Nested
    inner class ExceptionTest {
        @DisplayName("不包含根路由测试")
        @Test
        fun noRootPath() {
            val exp = "hashFiles('/data/a/a')"
            Assertions.assertThrows(RuntimeException::class.java) {
                ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, null, null).value
            }
            val exp1 = "hashFiles('D: \\data\\.\\..')"
            Assertions.assertThrows(RuntimeException::class.java) {
                ExpressionParser.createTree(exp1, null, nameValue, null)!!.evaluate(null, ev, null, null).value
            }
        }

        @DisplayName("不包含./..测试")
        @Test
        fun noIndexPath() {
            val exp = "hashFiles('/data/./..')"
            Assertions.assertThrows(RuntimeException::class.java) {
                ExpressionParser.createTree(exp, null, nameValue, null)!!.evaluate(null, ev, null, null).value
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "hashFiles('testData/sha255Hash') => 2f11d1771e05dde5d1c004cea5211741d5bbd4ac82704d714005ccb90f94d0e0",
            "hashFiles('**/sha**') => f7cd1e06c7fa507b32f2497beb0ab493c3fdf8a10f374209baf172035af4716a",
            "hashFiles('test?ata/sha255H???') => 2f11d1771e05dde5d1c004cea5211741d5bbd4ac82704d714005ccb90f94d0e0"
        ]
    )
    @Disabled
    fun evaluateCore(evaluate: String) {
        val (exp, expect) = evaluate.split(" => ")
        Assertions.assertEquals(
            expect,
            ExpressionParser.createTree(
                exp, null, nameValue,
                listOf(
                    FunctionInfo(
                        HashFilesFunction.name,
                        1,
                        Byte.MAX_VALUE.toInt(),
                        HashFilesFunction()
                    )
                )
            )!!.evaluate(null, ev, null, null).value
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "hashFiles(parameters.str, variables.xxx, '**/sha**') => hashFiles('12138', variables.xxx, '**/sha**')"
        ]
    )
    fun subNameValueEvaluateCore(subEvaluate: String) {
        val (exp, expect) = subEvaluate.split(" => ")
        val subInfo = SubNameValueEvaluateInfo()
        Assertions.assertEquals(
            expect,
            ExpressionParser.createSubNameValueEvaluateTree(
                exp, null, parametersNameValue,
                listOf(
                    FunctionInfo(
                        HashFilesFunction.name,
                        1,
                        Byte.MAX_VALUE.toInt(),
                        HashFilesFunction()
                    )
                ),
                subInfo
            )!!.subNameValueEvaluate(null, parametersEv, null, subInfo, null).value
        )
    }

    companion object {
        private val ev = ExecutionContext(DictionaryContextData())
        private val nameValue = mutableListOf<NamedValueInfo>()
        private val parametersNameValue = mutableListOf<NamedValueInfo>()
        private val parametersEv = ExecutionContext(DictionaryContextData())

        @BeforeAll
        @JvmStatic
        fun initData() {
            nameValue.add(NamedValueInfo("variables", ContextValueNode()))
            val workSpace = ClassPathResource("specialFunctions/hashFiles").url.file
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
            ev.expressionValues.add(
                "ci",
                DictionaryContextData().apply {
                    add(
                        "workspace",
                        StringContextData(workSpace)
                    )
                }
            )

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
            }
            parametersEv.expressionValues.add("parameters", parametersData)
        }
    }
}
