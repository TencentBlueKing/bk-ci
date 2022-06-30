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

package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.pojo.MavenGAVC
import com.tencent.bkrepo.maven.util.MavenStringUtils.resolverName
import org.apache.commons.lang3.StringUtils

object MavenGAVCUtils {

    /**
     *
     */
    fun String.mavenGAVC(): MavenGAVC {
        val pathList = this.trim('/').split("/")
        val version = pathList[pathList.size - 2]
        val artifactId = pathList[pathList.size - 3]
        val groupId = StringUtils.join(pathList.subList(0, pathList.size - 3), ".")
        return MavenGAVC(groupId, artifactId, version, null)
    }

    /**
     *
     */
    fun MavenArtifactInfo.toMavenGAVC(): MavenGAVC {
        val mavenVersion = this.jarName.resolverName(this.artifactId, this.versionId)
        return MavenGAVC(
            groupId = this.groupId,
            artifactId = this.artifactId,
            version = this.versionId,
            classifier = mavenVersion.classifier
        )
    }

    fun String.toMavenGAVC(): MavenGAVC {
        val paths = this.trim('/').split("/")
        val jarName = paths.last()
        val maven = this.mavenGAVC()
        val mavenVersion = jarName.resolverName(maven.artifactId, maven.version)
        return MavenGAVC(
            groupId = maven.groupId,
            artifactId = maven.artifactId,
            version = maven.version,
            classifier = mavenVersion.classifier
        )
    }
}
