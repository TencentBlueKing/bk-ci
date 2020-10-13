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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.helm.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@DisplayName("yaml工具类测试")
@SpringBootTest
class YamlUtilsTest {
    @Test
    @DisplayName("yaml转换为json测试")
    fun yaml2JsonTest() {
        val jsonString = YamlUtils.yaml2Json(yamlStr.byteInputStream())
        println(jsonString)
    }

    @Test
    fun convertStringToEntityTest() {
        val map = YamlUtils.convertStringToEntity<Map<String, Any>>(yamlStr)
        Assertions.assertEquals(map.size, 4)
    }

    companion object {
        val yamlStr = "apiVersion: v1\n" +
            "entries:\n" +
            "  bk-redis:\n" +
            "  - apiVersion: v1\n" +
            "    appVersion: '1.0'\n" +
            "    description: 这是一个测试示例\n" +
            "    name: bk-redis\n" +
            "    version: 0.1.1\n" +
            "    urls:\n" +
            "    - http://localhost:10021/test/helm-local/charts/bk-redis-0.1.1.tgz\n" +
            "    created: '2020-06-24T09:24:41.135Z'\n" +
            "    digest: e755d7482cb0422f9c3f7517764902c94bab7bcf93e79b6277c49572802bfba2\n" +
            "generated: '2020-06-24T09:26:05.026Z'\n" +
            "serverInfo: {}"
    }
}
