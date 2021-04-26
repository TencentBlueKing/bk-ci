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

package com.tencent.bkrepo.docker.helper

import com.tencent.bkrepo.docker.helpers.DockerCatalogTagsSlicer
import com.tencent.bkrepo.docker.helpers.DockerPaginationElementsHolder
import com.tencent.bkrepo.docker.response.CatalogResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DockerCatalogTagsSlicerTest {

    @Test
    @DisplayName("测试分页索引")
    fun testSliceCatalog() {
        val maxEntries = 3
        val lastEntry = "apache"
        val manifests = mapOf("nginx" to "v1", "linux" to "latest", "apache" to "v1", "java" to "v1")
        val elementsHolder = DockerPaginationElementsHolder()

        manifests.forEach {
            if (it.key.isNotBlank()) {
                elementsHolder.addElement(it.key)
            }
            DockerCatalogTagsSlicer.sliceCatalog(elementsHolder, maxEntries, lastEntry)
        }

        val catalogResponse = CatalogResponse(elementsHolder)
        Assertions.assertEquals(catalogResponse.repositories.size, 1)
    }
}
