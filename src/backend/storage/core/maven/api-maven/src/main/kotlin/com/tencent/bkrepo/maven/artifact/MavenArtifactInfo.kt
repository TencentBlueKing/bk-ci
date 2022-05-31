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

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX

class MavenArtifactInfo(
    projectId: String,
    repoName: String,
    artifactUri: String
) : ArtifactInfo(projectId, repoName, artifactUri) {

    lateinit var groupId: String
    lateinit var artifactId: String
    lateinit var versionId: String
    lateinit var jarName: String

    companion object {
        const val MAVEN_MAPPING_URI = "/{projectId}/{repoName}/**"
        const val MAVEN_EXT_DETAIL = "/version/detail/{projectId}/{repoName}"
        const val MAVEN_EXT_PACKAGE_DELETE = "/package/delete/{projectId}/{repoName}"
        const val MAVEN_EXT_VERSION_DELETE = "/version/delete/{projectId}/{repoName}"
    }

    private fun hasGroupId(): Boolean {
        return groupId.isNotBlank() && "NA" != groupId
    }

    private fun hasArtifactId(): Boolean {
        return artifactId.isNotBlank() && "NA" != artifactId
    }

    private fun hasVersion(): Boolean {
        return versionId.isNotBlank() && "NA" != versionId
    }

    fun isValid(): Boolean {
        return hasGroupId() && hasArtifactId() && hasVersion()
    }

    fun isSnapshot(): Boolean {
        return versionId.endsWith(SNAPSHOT_SUFFIX)
    }
}
