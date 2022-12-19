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

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.yaml.v2.models.Variable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Stream Yaml 构建测试")
internal class StreamYamlBuildTest {

    @DisplayName("手动触发参数获取测试")
    @Test
    fun getInputParamsTest() {
        val originData = mapOf(
            "string" to Variable(value = "123", allowModifyAtStartup = true),
            "test" to Variable(value = "123")
        )
        val testData = mapOf("string" to "456")
        val resultData = listOf(BuildParameters("variables.string", "456", BuildFormPropertyType.STRING))
        Assertions.assertEquals(resultData, StreamYamlBuild.getInputParams(originData, testData))
    }

    @DisplayName("手动触发参数获取测试(异常报错)")
    @Test
    fun getInputParamsExTest() {
        val originData = mapOf("string" to Variable(value = "123"), "test" to Variable(value = "123"))
        val testData = mapOf("string" to "456")
        Assertions.assertThrows(RuntimeException::class.java) { StreamYamlBuild.getInputParams(originData, testData) }
    }
}
