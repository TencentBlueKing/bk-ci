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

package com.tencent.bkrepo.docker.util

import com.tencent.bkrepo.docker.model.DockerDigest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders

@DisplayName("repoServiceUtilTest")
@SpringBootTest
class ResponseUtilTest {

    @Test
    @DisplayName("测试http头判断")
    fun putHasStreamTest() {
        val httpHeader = HttpHeaders()
        val result = ResponseUtil.putHasStream(httpHeader)
        Assertions.assertNotEquals(result, true)
        httpHeader.set("User-Agent", "Go-http-client/1.1")
        Assertions.assertNotEquals(result, true)
    }

    @Test
    @DisplayName("测试URI路径获取")
    fun getDockerURITest() {
        val httpHeader = HttpHeaders()
        val path = "/docker/nginx"
        var result = ResponseUtil.getDockerURI(path, httpHeader)
        Assertions.assertNotEquals(result.port, 0)
        Assertions.assertEquals(result.host, "localhost")
        httpHeader.set("Host", "127.0.0.1:80")
        result = ResponseUtil.getDockerURI(path, httpHeader)
        Assertions.assertNotEquals(result.host, "localhost")
    }

    @Test
    @DisplayName("测试空的blob文件")
    fun isEmptyBlobTest() {
        var digest = DockerDigest("sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d2")
        Assertions.assertEquals(ResponseUtil.isEmptyBlob(digest), false)
        digest = DockerDigest("sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4")
        Assertions.assertEquals(ResponseUtil.isEmptyBlob(digest), true)
    }
}
