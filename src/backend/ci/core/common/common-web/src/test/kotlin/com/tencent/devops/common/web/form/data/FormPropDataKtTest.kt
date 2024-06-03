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

package com.tencent.devops.common.web.form.data

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("生成前端动态表单Props项工具类测试")
internal class FormPropDataKtTest {

    companion object {
        private val formData = InputPropData("", FormDataType.STRING, "", "")
    }

    @DisplayName("测试都不为空的结果")
    @Test
    fun testAllNotNull() {
        val data = mapOf(
            "data" to listOf("aaa"),
            "muta" to true
        )
        Assertions.assertEquals(
            data, formData.buildProps(
                data
            )
        )
    }

    @DisplayName("测试值为空的结果")
    @Test
    fun testValueNull() {
        val data = mapOf(
            "data" to emptyList<String>(),
            "muta" to null,
            "aaa" to mapOf<String, String>(),
            "ccc" to "!23"
        )
        Assertions.assertEquals(
            mapOf("ccc" to "!23"), formData.buildProps(
                data
            )
        )
    }

    @DisplayName("测试值全为空的结果")
    @Test
    fun testAllNull() {
        val data = null
        Assertions.assertEquals(
            data, formData.buildProps(
                data
            )
        )
    }
}
