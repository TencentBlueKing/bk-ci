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

package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.npm.utils.GsonUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@DisplayName("npm服务测试")
class NpmServiceTest {
    @Autowired
    private lateinit var wac: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    @Test
    @DisplayName("npm包信息查询测试")
    fun searchPackageInfoTest() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/test/npm-local/babel-core/latest")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        ).andExpect { MockMvcResultMatchers.status().isOk }
            .andDo(MockMvcResultHandlers.print()).andReturn()
        val resultMap = GsonUtils.gsonToMaps<Any>(result.response.contentAsString)!!
        Assertions.assertEquals(resultMap["version"], "6.26.3")
        Assertions.assertEquals(resultMap["name"], "babel-core")
        Assertions.assertEquals(resultMap["_id"], "babel-core@6.26.3")
    }

    // {projectId}/{repoName}/-/package/**/dist-tags
    @Test
    @DisplayName("npm search测试")
    fun searchTest() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/test/npm-local/-/package/babel-core/dist-tags")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        ).andExpect { MockMvcResultMatchers.status().isOk }
            .andDo(MockMvcResultHandlers.print()).andReturn()
        val resultMap = GsonUtils.gsonToMaps<Any>(result.response.contentAsString)!!
        Assertions.assertEquals(resultMap["latest"], "6.26.3")
        Assertions.assertEquals(resultMap["old"], "5.8.38")
        Assertions.assertEquals(resultMap["bridge"], "7.0.0-bridge.0")
    }
}
