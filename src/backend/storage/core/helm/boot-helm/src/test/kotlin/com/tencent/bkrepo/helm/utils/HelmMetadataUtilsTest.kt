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

package com.tencent.bkrepo.helm.utils

import com.tencent.bkrepo.common.artifact.api.FileSystemArtifactFile
import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@DisplayName("helmMeta工具类测试")
@SpringBootTest
class HelmMetadataUtilsTest {
    @Test
    @DisplayName("HelmChartMetadata转换为Map测试")
    fun toMapTest() {
        val file = File("/XXXXX/test/helm-local/tomcat-0.4.1.tgz")
        val arFile = FileSystemArtifactFile(file)
        val chartMetadata = ChartParserUtil.parseChartFileInfo(arFile)
        println(HelmMetadataUtils.convertToMap(chartMetadata))
    }

    @Test
    @DisplayName("map转换为HelmChartMetadata测试")
    fun toObjectTest() {
        val file = File("/XXXX/test/helm-local/tomcat-0.4.1.tgz")
        val arFile = FileSystemArtifactFile(file)
        val chartMetadata = ChartParserUtil.parseChartFileInfo(arFile)
        val map = HelmMetadataUtils.convertToMap(chartMetadata)
        val newChartMetadata = HelmMetadataUtils.convertToObject(map)
        Assertions.assertEquals(chartMetadata, newChartMetadata)
    }
}
