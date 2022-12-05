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

package com.tencent.bkrepo.interceptor

import com.tencent.bkrepo.common.artifact.exception.ArtifactDownloadForbiddenException
import com.tencent.bkrepo.common.artifact.interceptor.impl.FilenameInterceptor
import com.tencent.bkrepo.common.artifact.interceptor.impl.MetadataInterceptor
import com.tencent.bkrepo.common.artifact.interceptor.impl.WebInterceptor
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class DownloadInterceptorTest {

    @Test
    @DisplayName("文件名下载拦截器测试")
    fun filenameTest() {
        val invalidRule = mapOf<String, Any>(
            "k" to ""
        )
        val passRule = mapOf<String, Any>(
            FILENAME to "*.txt"
        )
        val forbiddenRule = mapOf<String, Any>(
            FILENAME to "*.apk"
        )
        val nodeDetail = nodeDetail("test.txt")
        assertDoesNotThrow { FilenameInterceptor(invalidRule).intercept(nodeDetail) }
        assertDoesNotThrow { FilenameInterceptor(passRule).intercept(nodeDetail) }
        assertThrows<ArtifactDownloadForbiddenException> { FilenameInterceptor(forbiddenRule).intercept(nodeDetail) }
    }

    @Test
    @DisplayName("元数据下载拦截器测试")
    fun metadataTest() {
        val invalidRule = mapOf<String, Any>(
            METADATA to "k：v"
        )
        val passRule = mapOf<String, Any>(
            METADATA to "k1:  v1"
        )
        val forbiddenRule = mapOf<String, Any>(
            METADATA to "k: v"
        )
        val metadata = mapOf(
            "k1" to "v1",
            "k2" to "v2"
        )
        val nodeDetail = nodeDetail("test", metadata)
        assertDoesNotThrow { MetadataInterceptor(invalidRule).intercept(nodeDetail) }
        assertDoesNotThrow { MetadataInterceptor(passRule).intercept(nodeDetail) }
        assertThrows<ArtifactDownloadForbiddenException> { MetadataInterceptor(forbiddenRule).intercept(nodeDetail) }
    }

    @Test
    @DisplayName("Web端下载拦截器测试")
    fun webTest() {
        val invalidRule = mapOf<String, Any>()
        val passRule = mapOf<String, Any>(
            FILENAME to "*",
            METADATA to "k1:  v1"
        )
        val forbiddenRule = mapOf<String, Any>(
            FILENAME to "**.apk",
            METADATA to "k1:  v1"
        )
        val metadata = mapOf(
            "k1" to "v1",
            "k2" to "v2"
        )
        val nodeDetail = nodeDetail("test.txt", metadata)
        assertDoesNotThrow { WebInterceptor(invalidRule).intercept(nodeDetail) }
        assertDoesNotThrow { WebInterceptor(passRule).intercept(nodeDetail) }
        assertThrows<ArtifactDownloadForbiddenException> { WebInterceptor(forbiddenRule).intercept(nodeDetail) }
    }

    @Test
    @DisplayName("移动端下载拦截器测试")
    fun mobileTest() {
        val invalidRule = mapOf<String, Any>(
            "k" to "v"
        )
        val passRule = mapOf<String, Any>(
            FILENAME to "**",
            METADATA to "k1:  v1"
        )
        val forbiddenRule = mapOf<String, Any>(
            FILENAME to "**.apk",
            METADATA to "k1:  v1"
        )
        val metadata = mapOf(
            "k1" to "v1",
            "k2" to "v2"
        )
        val nodeDetail = nodeDetail("test", metadata)
        assertDoesNotThrow { WebInterceptor(invalidRule).intercept(nodeDetail) }
        assertDoesNotThrow { WebInterceptor(passRule).intercept(nodeDetail) }
        assertThrows<ArtifactDownloadForbiddenException> { WebInterceptor(forbiddenRule).intercept(nodeDetail) }
    }

    private fun nodeDetail(name: String, metadata: Map<String, Any>? = null): NodeDetail {
        val path = "/a/b/c"
        val nodeInfo = NodeInfo(
            createdBy = UT_USER,
            createdDate = LocalDateTime.now().toString(),
            lastModifiedBy = UT_USER,
            lastModifiedDate = LocalDateTime.now().toString(),
            folder = false,
            path = path,
            name = name,
            fullPath = "$path/$name",
            size = 1,
            projectId = UT_PROJECT,
            repoName = UT_REPO,
            metadata = metadata
        )
        return NodeDetail(nodeInfo)
    }


    companion object {
        private const val FILENAME = "filename"
        private const val METADATA = "metadata"
        private const val UT_USER = "ut_user"
        private const val UT_PROJECT = "ut_project"
        private const val UT_REPO = "ut_repo"
    }
}
