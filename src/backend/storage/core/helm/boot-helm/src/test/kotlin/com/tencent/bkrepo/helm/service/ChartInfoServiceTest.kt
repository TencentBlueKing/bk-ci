/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.helm.service

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("chart info 信息列表测试")
@SpringBootTest
class ChartInfoServiceTest {
    @Autowired
    private lateinit var wac: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    @Test
    @DisplayName("chart列表展示")
    fun allChartsListTest() {
        val perform =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/test/test/api/charts")
                    .header("Authorization", "Basic XXXXX=")
                    .contentType(MediaType.APPLICATION_JSON)
            )
        perform.andExpect { MockMvcResultMatchers.status().is4xxClientError }
        perform.andExpect { MockMvcResultMatchers.status().isOk }
        println(perform.andReturn().response.contentAsString)
    }

    @Test
    @DisplayName("json转换查询测试")
    fun searchJsonTest() {
        val str = "apiVersion: v1\n" +
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
            "  mychart:\n" +
            "  - apiVersion: v2\n" +
            "    appVersion: 1.16.0\n" +
            "    description: A Helm chart for Kubernetes\n" +
            "    name: mychart\n" +
            "    type: application\n" +
            "    version: 0.1.2\n" +
            "    urls:\n" +
            "    - http://localhost:10021/test/helm-local/charts/mychart-0.1.2.tgz\n" +
            "    created: '2020-06-24T09:24:47.802Z'\n" +
            "    digest: 8dedfa1d0e7ff20dfb3ef3c9b621f43f2e89f3e7361005639510ab10329d1ec8\n" +
            "generated: '2020-06-24T09:26:05.026Z'\n" +
            "serverInfo: {}"
        val indexYamlMetadata = str.readJsonString<HelmIndexYamlMetadata>()
        Assertions.assertEquals(indexYamlMetadata.entries.size, 2)
        Assertions.assertEquals(indexYamlMetadata.entries["/mychart"]?.size, 1)
    }

    @Test
    @DisplayName("获取chart返回状态测试")
    fun chartExistsTest() {
        val perform =
            mockMvc.perform(
                MockMvcRequestBuilders.head("/test/test/api/charts/mongodb/7.8.10")
                    .header("Authorization", "Basic XXXXX=")
                    .contentType(MediaType.APPLICATION_JSON)
            )
        println(perform.andReturn().response.contentAsString)
        perform.andExpect { MockMvcResultMatchers.status().isOk }
        val status = perform.andReturn().response.status
        Assertions.assertEquals(status, 200)
    }
}
