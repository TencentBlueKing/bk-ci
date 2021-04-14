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

package com.tencent.bkrepo.maven.artifact

import com.tencent.bkrepo.common.api.util.readXmlString
import com.tencent.bkrepo.maven.pojo.MavenPom
import com.tencent.bkrepo.maven.pojo.MavenSnapshot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import java.io.FileInputStream

class MavenArtifactInfoResolverTest {

    private val project = "bkrepo"
    private val repo = "maven"
    private val artifactUri = "org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar"

    @Test
    fun resolverTest() {
        val request = MockHttpServletRequest()
        val mavenArtifactInfo = MavenArtifactInfoResolver().resolve(
            project,
            repo,
            artifactUri,
            request
        )
        Assertions.assertEquals("org.slf4j", mavenArtifactInfo.groupId)
        Assertions.assertEquals("slf4j-api", mavenArtifactInfo.artifactId)
        Assertions.assertEquals("1.7.30", mavenArtifactInfo.versionId)
    }

    @Test
    fun mavenPomTest1() {
        val fileInputStream = FileInputStream("/Users/Weaving/Downloads/spring-boot-build-1.0.0.RELEASE.pom")
        val mavenPom = fileInputStream.readXmlString<MavenPom>()
        Assertions.assertEquals("1.0.0.RELEASE", mavenPom.version)
    }

    @Test
    fun mavenPomTes2t() {
        val fileInputStream = FileInputStream("/Users/Weaving/Downloads/bksdk-1.0.0-20200928.015515-1 (1).pom")
        val mavenPom = fileInputStream.readXmlString<MavenPom>()
        Assertions.assertEquals("1.0.0-SNAPSHOT", mavenPom.version)
    }

    @Test
    fun mavenSnapshotTest() {
        val fileInputStream = FileInputStream("/Users/Weaving/Downloads/maven-metadata.xml.1")
        val mavenSnapshot = fileInputStream.readXmlString<MavenSnapshot>()
        Assertions.assertEquals(
            "1.0.0-20200928.033656-1",
            mavenSnapshot.versioning.snapshotVersions[0].value
        )
    }
}
