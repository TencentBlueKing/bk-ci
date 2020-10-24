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

package com.tencent.bkrepo.helm.service

import com.tencent.bkrepo.helm.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.utils.JsonUtil
import org.junit.jupiter.api.AfterEach
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
import java.time.LocalDateTime

@DisplayName("chart info 信息列表测试")
@SpringBootTest
class ChartInfoServiceTest {
    @Autowired
    private lateinit var chartInfoService: ChartInfoService

    @Autowired
    private lateinit var wac: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    private val projectId = "test"
    private var repoName = "helm-local"

    @AfterEach
    fun tearDown() {}

    @Test
    @DisplayName("chart列表展示")
    fun allChartsListTest() {
        val helmArtifactInfo = HelmArtifactInfo(projectId, repoName, "/")
        val allChartsList = chartInfoService.allChartsList(helmArtifactInfo, LocalDateTime.now())
        Assertions.assertEquals(allChartsList.size, 0)
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
        val searchJson = JsonUtil.searchJson(str.byteInputStream(), "/")
        Assertions.assertEquals(searchJson.size, 2)
        val result = JsonUtil.searchJson(str.byteInputStream(), "/mychart")
        Assertions.assertEquals(result.size, 1)
        val resultJson = JsonUtil.searchJson(str.byteInputStream(), "/mychart/0.1.0")
        Assertions.assertEquals(resultJson["error"], "no chart version found for mychart-0.1.0")
    }

    @Test
    @DisplayName("获取chart返回状态测试")
    fun chartExistsTest() {
        val perform =
            mockMvc.perform(
                MockMvcRequestBuilders.head("/test/helm-local/api/charts/bk-redis/0.1.1").header("Authorization", "Basic eHdoeToxMjM0NTY=").contentType(
                    MediaType.APPLICATION_JSON_UTF8
                )
            )
        perform.andExpect { MockMvcResultMatchers.status().isOk }
        val status = perform.andReturn().response.status
        Assertions.assertEquals(status, 200)
    }
}
