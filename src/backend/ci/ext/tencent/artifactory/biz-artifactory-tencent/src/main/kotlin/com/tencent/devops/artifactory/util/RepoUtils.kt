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

package com.tencent.devops.artifactory.util

import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.pojo.PackageVersionInfo
import com.tencent.devops.common.archive.pojo.QueryNodeInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RepoUtils {
    const val PIPELINE_REPO = "pipeline"
    const val CUSTOM_REPO = "custom"
    const val REPORT_REPO = "report"
    const val LOG_REPO = "log"
    const val IMAGE_REPO = "image"

    fun getRepoByType(repoType: ArtifactoryType): String {
        return when (repoType) {
            ArtifactoryType.PIPELINE -> PIPELINE_REPO
            ArtifactoryType.CUSTOM_DIR -> CUSTOM_REPO
            ArtifactoryType.IMAGE -> IMAGE_REPO
            ArtifactoryType.REPORT -> REPORT_REPO
        }
    }

    fun getTypeByRepo(repo: String): ArtifactoryType {
        return when (repo) {
            PIPELINE_REPO -> ArtifactoryType.PIPELINE
            CUSTOM_REPO -> ArtifactoryType.CUSTOM_DIR
            IMAGE_REPO -> ArtifactoryType.IMAGE
            else -> throw IllegalArgumentException("invalid repo: $repo")
        }
    }

    fun isPipelineFile(nodeInfo: NodeInfo): Boolean {
        return nodeInfo.repoName == PIPELINE_REPO
    }

    fun isPipelineFile(nodeInfo: QueryNodeInfo): Boolean {
        return nodeInfo.repoName == PIPELINE_REPO
    }

    fun isImageFile(nodeInfo: QueryNodeInfo): Boolean {
        return nodeInfo.repoName == IMAGE_REPO
    }

    fun toFileInfo(fileInfo: NodeInfo): FileInfo {
        val fullPath = refineFullPath(fileInfo)
        return FileInfo(
            name = fileInfo.name,
            fullName = fullPath,
            path = fileInfo.path,
            fullPath = fullPath,
            size = if (fileInfo.folder) -1 else fileInfo.size,
            folder = fileInfo.folder,
            modifiedTime = LocalDateTime.parse(fileInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            artifactoryType = getTypeByRepo(fileInfo.repoName)
        )
    }

    fun toFileInfo(fileInfo: com.tencent.bkrepo.generic.pojo.FileInfo): FileInfo {
        val fullPath = refineFullPath(fileInfo)
        return FileInfo(
            name = fileInfo.name,
            fullName = fullPath,
            path = fileInfo.path,
            fullPath = fullPath,
            size = if (fileInfo.folder) -1 else fileInfo.size,
            folder = fileInfo.folder,
            modifiedTime = LocalDateTime.parse(fileInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            artifactoryType = ArtifactoryType.CUSTOM_DIR
        )
    }

    fun toFileDetail(nodeDetail: NodeDetail, shortUrl: String? = null): FileDetail {
        with(nodeDetail) {
            return FileDetail(
                name = name,
                path = path,
                fullName = fullPath,
                fullPath = fullPath,
                size = size,
                createdTime = LocalDateTime.parse(createdDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                modifiedTime = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                checksums = FileChecksums(sha256, "", md5 ?: ""),
                meta = metadata.entries.associate { Pair(it.key, it.value.toString()) },
                nodeMetadata = nodeMetadata,
                url = "/bkrepo/api/user/generic/$projectId/$repoName$fullPath?download=true",
                shortUrl = shortUrl
            )
        }
    }

    fun toFileDetail(nodeInfo: QueryNodeInfo): FileDetail {
        return FileDetail(
            name = nodeInfo.name,
            path = nodeInfo.path,
            fullName = nodeInfo.fullPath,
            fullPath = nodeInfo.fullPath,
            size = nodeInfo.size,
            createdTime = LocalDateTime.parse(nodeInfo.createdDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            modifiedTime = LocalDateTime.parse(nodeInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            checksums = FileChecksums(
                sha256 = nodeInfo.sha256 ?: "",
                sha1 = "",
                md5 = nodeInfo.md5 ?: ""
            ),
            meta = nodeInfo.metadata ?: mapOf()
        )
    }

    fun toFileDetail(imageName: String, packageVersionInfo: PackageVersionInfo): FileDetail {
        with(packageVersionInfo) {
            return FileDetail(
                name = imageName,
                fullName = "$imageName:${basic.version}",
                path = basic.fullPath,
                fullPath = basic.fullPath,
                size = basic.size,
                createdTime = LocalDateTime.parse(basic.createdDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                modifiedTime = LocalDateTime.parse(basic.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                checksums = FileChecksums(basic.sha256, "", basic.md5),
                meta = metadata.associate { it["key"].toString() to it["value"].toString() }
            )
        }
    }

    private fun refineFullPath(fileInfo: com.tencent.bkrepo.generic.pojo.FileInfo): String {
        return if (fileInfo.folder && !fileInfo.fullPath.endsWith("/")) {
            fileInfo.fullPath + "/"
        } else {
            fileInfo.fullPath
        }
    }

    private fun refineFullPath(nodeInfo: NodeInfo): String {
        return if (nodeInfo.folder && !nodeInfo.fullPath.endsWith("/")) {
            nodeInfo.fullPath + "/"
        } else {
            nodeInfo.fullPath
        }
    }
}
