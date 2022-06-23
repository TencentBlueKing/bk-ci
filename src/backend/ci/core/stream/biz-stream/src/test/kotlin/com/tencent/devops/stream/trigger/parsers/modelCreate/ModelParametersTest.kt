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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.stream.trigger.parsers.modelCreate.ModelParameters.addInputParams
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("创建model参数测试")
internal class ModelParametersTest {

    @DisplayName("测试inputs修改参数")
    @Test
    fun inputsParamsTest() {
        val originData = mutableListOf(
            BuildFormProperty(
                id = "string",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = "str",
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "array",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = "[1, 2]",
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
        )
        val testData = mapOf(
            "string" to "string",
            "int" to "123",
            "double" to "4.5",
            "array" to "[ 1, 2.2, \"3\" ]",
            "bool" to "true"
        )

        val resultData = mutableListOf(
            BuildFormProperty(
                id = "string",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = "string",
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "array",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = "[ 1, 2.2, \"3\" ]",
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
        )

        Assertions.assertEquals(resultData, originData.addInputParams(null, testData))
    }

    @DisplayName("测试inputs修改参数(异常报错)")
    @Test
    fun inputsParamsExceptionTest() {
        val originData = mutableListOf(
            BuildFormProperty(
                id = "string",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = "str",
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "array",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = "[1, 2]",
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null,
                readOnly = true
            )
        )
        val testData = mapOf(
            "string" to "string",
            "int" to "123",
            "double" to "4.5",
            "array" to "[ 1, 2.2, \"3\" ]",
            "bool" to "true"
        )
        val variables = mapOf(
            "string" to Variable(
                ""
            )
        )

        Assertions.assertThrows(RuntimeException::class.java) { originData.addInputParams(variables, testData) }
    }
}
