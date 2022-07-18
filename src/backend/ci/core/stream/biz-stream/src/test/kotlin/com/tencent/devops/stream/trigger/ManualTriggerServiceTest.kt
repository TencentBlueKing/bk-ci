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

package com.tencent.devops.stream.trigger

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.VariableDatasource
import com.tencent.devops.process.yaml.v2.models.VariablePropOption
import com.tencent.devops.process.yaml.v2.models.VariablePropType
import com.tencent.devops.process.yaml.v2.models.VariableProps
import com.tencent.devops.stream.trigger.ManualTriggerService.Companion.stringToOther
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

@DisplayName("stream手动触发相关测试")
internal class ManualTriggerServiceTest {

    @DisplayName("测试创建stream variables的动态表单")
    @Test
    fun parseVariablesToFormTest() {
        val variables = mutableMapOf(
            "旧数据或不展示的" to Variable("旧数据或不展示的"),
            "默认展示的，无特殊类型" to Variable("默认展示的，无特殊类型-v", allowModifyAtStartup = true)
        )

        VariablePropType.values().forEach { type ->
            when (type) {
                VariablePropType.VUEX_INPUT -> variables["input类型"] = Variable(
                    "input类型-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        label = "input类型",
                        type = VariablePropType.VUEX_INPUT.value
                    )
                )
                VariablePropType.VUEX_TEXTAREA -> variables["input类型-text"] = Variable(
                    "input类型-text-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        type = VariablePropType.VUEX_TEXTAREA.value
                    )
                )
                VariablePropType.SELECTOR -> {
                    variables["select类型"] = Variable(
                        "select类型-v",
                        allowModifyAtStartup = true,
                        props = VariableProps(
                            type = VariablePropType.SELECTOR.value,
                            options = listOf(
                                VariablePropOption(
                                    id = "select类型-v"
                                ),
                                VariablePropOption(
                                    id = 123,
                                    label = "第二个"
                                ),
                                VariablePropOption(
                                    id = true,
                                    description = "第三个"
                                )
                            )
                        )
                    )
                    variables["select类型-url"] = Variable(
                        "select类型-v",
                        allowModifyAtStartup = true,
                        props = VariableProps(
                            type = VariablePropType.SELECTOR.value,
                            datasource = VariableDatasource(
                                "", "", "", "", false, "", ""
                            ),
                            multiple = true,
                            required = true
                        )
                    )
                }
                VariablePropType.CHECKBOX -> variables["checkBox类型"] = Variable(
                    "checkBox类型-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        type = VariablePropType.CHECKBOX.value,
                        options = listOf(
                            VariablePropOption(
                                id = "checkBox类型-v"
                            ),
                            VariablePropOption(
                                id = 123,
                                label = "第二个"
                            ),
                            VariablePropOption(
                                id = true,
                                description = "第三个"
                            )
                        )
                    )
                )
                VariablePropType.BOOLEAN -> variables["boolean类型"] = Variable(
                    "boolean类型-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        type = VariablePropType.BOOLEAN.value
                    )
                )
                VariablePropType.TIME_PICKER -> variables["time类型"] = Variable(
                    "time类型-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        type = VariablePropType.TIME_PICKER.value
                    )
                )
                VariablePropType.COMPANY_STAFF_INPUT -> variables["company-staff-input类型"] = Variable(
                    "company-staff-input类型-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        type = VariablePropType.COMPANY_STAFF_INPUT.value
                    )
                )
                VariablePropType.TIPS -> variables["tips类型"] = Variable(
                    "tips类型-v",
                    allowModifyAtStartup = true,
                    props = VariableProps(
                        type = VariablePropType.TIPS.value
                    )
                )
            }
        }

        val form = ManualTriggerService.parseVariablesToForm(variables)

        Assertions.assertEquals(
            JsonUtil.getObjectMapper().readTree(
                ClassPathResource("parseVariablesToFormTestResult.json").inputStream
            ),
            JsonUtil.getObjectMapper().readTree(JsonUtil.toJson(form))
        )
    }

    @DisplayName("测试string转其他类型")
    @Test
    fun stringToOtherTest() {
        val testData = "1,true,1.2,1.x,xxx,1xx,truexx,false , 1.2.3, 2.0 "
        val expectData = setOf(1L, true, 1.2, "1.x", "xxx", "1xx", "truexx", false, "1.2.3", 2.0)
        Assertions.assertEquals(
            expectData,
            testData.split(",").map { it.trim().stringToOther() }.toSet()
        )
    }

    @DisplayName("解析手动触发输入参数测试")
    @Test
    fun parseInputsTest() {
        val testData = mapOf(
            "string" to "string",
            "int" to 123,
            "double" to 4.5,
            "array-single" to listOf("12"),
            "array" to listOf(1, 2.2, "3", "xxx"),
            "bool" to true
        )

        Assertions.assertEquals(
            mapOf(
                "string" to "string",
                "int" to "123",
                "double" to "4.5",
                "array-single" to "12",
                "array" to "1,2.2,3,xxx",
                "bool" to "true"
            ),
            ManualTriggerService.parseInputs(testData)
        )
    }
}
