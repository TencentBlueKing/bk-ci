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

package com.tencent.devops.common.web.form

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.form.data.CheckboxPropData
import com.tencent.devops.common.web.form.data.FormDataType
import com.tencent.devops.common.web.form.data.InputPropData
import com.tencent.devops.common.web.form.data.InputPropType
import com.tencent.devops.common.web.form.data.RadioPropData
import com.tencent.devops.common.web.form.data.SelectPropData
import com.tencent.devops.common.web.form.data.TimePropData
import com.tencent.devops.common.web.form.models.ui.DataSourceItem
import com.tencent.devops.common.web.form.models.ui.components.RadioComponent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("生成前端动态表单测试")
internal class FormBuilderTest {

    private val inputTestResData = """
{
    "title": "input-test",
    "description": "input-desc-test",
    "type": "object",
    "properties": {
        "input-test-id": {
            "type": "string",
            "title": "input表单测试",
            "default": "123",
            "required": false,
            "ui:component": {
                "name": "input"
            }
        }
    }
}
        """.trimIndent()

    @DisplayName("测试input表单")
    @Test
    fun inputFromTest() {
        val form = FormBuilder().setTitle("input-test").setDescription("input-desc-test")
            .setProp(
                InputPropData(
                    id = "input-test-id",
                    type = FormDataType.STRING,
                    title = "input表单测试",
                    default = "123"
                )
            ).build()

        assertJson(inputTestResData, form)
    }

    private val inputTextareaTestResData = """
{
    "title": "input-test",
    "description": "input-desc-test",
    "type": "object",
    "properties": {
        "input-test-id": {
            "type": "string",
            "title": "input表单测试",
            "default": "123",
            "required": false,
            "ui:component": {
                "name": "input",
                "props": {
                    "type": "textarea"
                }
            }
        }
    }
}
        """.trimIndent()

    @DisplayName("测试input-textarea表单")
    @Test
    fun inputTextareaFromTest() {
        val form = FormBuilder().setTitle("input-test").setDescription("input-desc-test")
            .setProp(
                InputPropData(
                    id = "input-test-id",
                    type = FormDataType.STRING,
                    title = "input表单测试",
                    default = "123",
                    inputType = InputPropType.TEXTAREA
                )
            ).build()

        assertJson(inputTextareaTestResData, form)
    }

    private val selectTestResData = """
{
    "title": "select-test",
    "description": "select-desc-test",
    "type": "object",
    "properties": {
        "select-test-id": {
            "type": "string",
            "title": "select表单测试",
            "default": "official",
            "required": true,
            "ui:component": {
                "name": "select",
                "props": {
                    "datasource": [
                        {
                            "value": "official",
                            "label": "官方插件"
                        },
                        {
                            "value": "external",
                            "label": "第三方插件"
                        }
                    ],
                    "multiple": false
                }
            }
        }
    }
}
        """.trimIndent()

    @DisplayName("测试select表单")
    @Test
    fun selectFromTest() {
        val form = FormBuilder().setTitle("select-test").setDescription("select-desc-test")
            .setProp(
                SelectPropData(
                    id = "select-test-id",
                    type = FormDataType.STRING,
                    title = "select表单测试",
                    default = "official",
                    required = true,
                    dataSource = listOf(
                        DataSourceItem("官方插件", "official"),
                        DataSourceItem("第三方插件", "external")
                    ),
                    multiple = false
                )
            ).build()

        assertJson(selectTestResData, form)
    }

    private val checkboxTestResData = """
{
    "title": "checkbox-test",
    "description": "checkbox-desc-test",
    "type": "object",
    "properties": {
        "checkbox-test-id": {
            "type": "array",
            "title": "checkbox表单测试",
            "default": "official",
            "required" : false,
            "ui:component": {
                "name": "checkbox",
                "props": {
                    "datasource": [
                        {
                            "value": "official",
                            "label": "官方插件"
                        },
                        {
                            "value": "external",
                            "label": "第三方插件"
                        }
                    ]
                }
            }
        }
    }
}
        """.trimIndent()

    @DisplayName("测试checkbox表单")
    @Test
    fun checkboxFromTest() {
        val form = FormBuilder().setTitle("checkbox-test").setDescription("checkbox-desc-test")
            .setProp(
                CheckboxPropData(
                    id = "checkbox-test-id",
                    type = FormDataType.ARRAY,
                    title = "checkbox表单测试",
                    default = "official",
                    dataSource = listOf(
                        DataSourceItem("官方插件", "official"),
                        DataSourceItem("第三方插件", "external")
                    )
                )
            ).build()

        assertJson(checkboxTestResData, form)
    }

    private val radioTestResData = """
{
    "title": "radio-test",
    "description": "radio-desc-test",
    "type": "object",
    "properties": {
        "radio-test-id": {
            "type": "boolean",
            "title": "radio表单测试",
            "default": true,
            "required" : false,
            "ui:component": {
                "name": "radio",
                "props": {
                    "datasource": [
                        {
                            "value": true,
                            "label": "是"
                        },
                        {
                            "value": false,
                            "label": "否"
                        }
                    ]
                }
            }
        }
    }
}
        """.trimIndent()

    @DisplayName("测试radio表单")
    @Test
    fun radioFromTest() {
        val form = FormBuilder().setTitle("radio-test").setDescription("radio-desc-test")
            .setProp(
                RadioPropData(
                    id = "radio-test-id",
                    type = FormDataType.BOOLEAN,
                    title = "radio表单测试",
                    default = true,
                    dataSource = listOf(
                        DataSourceItem("是", true),
                        DataSourceItem("否", false)
                    )
                )
            ).build()

        assertJson(radioTestResData, form)
    }

    private val timeTestResData = """
{
    "title": "time-test",
    "description": "time-desc-test",
    "type": "object",
    "properties": {
        "time-test-id": {
            "type": "string",
            "title": "time表单测试",
            "required" : false,
            "ui:component": {
                "name": "bk-time-picker",
                "props" : {
                  "type" : "time"
                }
            }
        }
    }
}
        """.trimIndent()

    @DisplayName("测试time表单")
    @Test
    fun timeFromTest() {
        val form = FormBuilder().setTitle("time-test").setDescription("time-desc-test")
            .setProp(
                TimePropData(
                    id = "time-test-id",
                    type = FormDataType.STRING,
                    title = "time表单测试"
                )
            ).build()

        assertJson(timeTestResData, form)
    }

    companion object {
        private fun assertJson(expectJson: Any, json: Any) {
            val objectMapper = JsonUtil.getObjectMapper()
            val expect = if (expectJson is String) {
                objectMapper.readTree(expectJson)
            } else {
                val jsonText = JsonUtil.toJson(expectJson)
                objectMapper.readTree(jsonText)
            }

            val j = if (json is String) {
                objectMapper.readTree(json)
            } else {
                val jsonText = JsonUtil.toJson(json)
                objectMapper.readTree(jsonText)
            }

            Assertions.assertTrue(expect.equals(j))
        }
    }
}
