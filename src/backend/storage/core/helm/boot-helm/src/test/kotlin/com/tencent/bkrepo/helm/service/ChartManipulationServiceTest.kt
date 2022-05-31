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

import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("chart上传/删除操作测试")
@SpringBootTest
class ChartManipulationServiceTest {
    @Autowired
    private lateinit var wac: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    @Test
    @DisplayName("chart上传")
    fun uploadTest() {
        val file = File("/XXXXX/test/helm-local/mysql-1.5.0.tgz")
        val mockFile = MockMultipartFile("chart", "mysql-1.5.0.tgz", ",multipart/form-data", file.inputStream())
        val perform =
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/api/test/test/charts")
                    .file(mockFile)
                    .header("Authorization", "Basic XXXXX=")
            )
        perform.andExpect { MockMvcResultMatchers.status().is4xxClientError }
        perform.andExpect { MockMvcResultMatchers.status().isOk }
        println(perform.andReturn().response.contentAsString)
    }

    @Test
    @DisplayName("删除版本")
    fun deleteVersionTest() {
        val perform =
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/test/test//api/charts/mysql/1.5.0").header(
                    "Authorization",
                    "Basic XXXXXX="
                ).contentType(MediaType.APPLICATION_JSON)
            )
        perform.andExpect { MockMvcResultMatchers.status().is4xxClientError }
        perform.andExpect { MockMvcResultMatchers.status().isOk }
        println(perform.andReturn().response.contentAsString)
    }
}
