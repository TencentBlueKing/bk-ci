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
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.bkrepo.ArtifactInfo
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BkRepoUtils {
    const val BKREPO_DEFAULT_USER = "admin"
    const val BKREPO_DEVOPS_PROJECT_ID = "devops"
    const val BKREPO_STORE_PROJECT_ID = "bk-store"
    const val BKREPO_STATIC_PROJECT_ID = "bkcdn"
    const val BKREPO_COMMOM_REPO = "common"

    const val REPO_NAME_PIPELINE = "pipeline"
    const val REPO_NAME_CUSTOM = "custom"
    const val REPO_NAME_IMAGE = "image"
    const val REPO_NAME_REPORT = "report"
    const val REPO_NAME_PLUGIN = "plugin"
    const val REPO_NAME_STATIC = "static"

    fun parseArtifactoryInfo(path: String): ArtifactInfo {
        val normalizedPath = path.trim().removePrefix("/").removePrefix("./")
        val roads = normalizedPath.split("/")
        if (roads.size < 3) throw InvalidParamException("invalid path $path")
        val projectId = roads[0]
        val repoName = checkRepoName(roads[1])
        val artifactUri = normalizedPath.removePrefix("$projectId/$repoName")
        val fileName = roads.last()
        return ArtifactInfo(projectId, repoName, artifactUri, fileName)
    }

    private fun checkRepoName(repoName: String): String {
        val validateRepoName =
            repoName == REPO_NAME_PIPELINE ||
                repoName == REPO_NAME_CUSTOM ||
                repoName == REPO_NAME_REPORT ||
                repoName == REPO_NAME_PLUGIN ||
                repoName == REPO_NAME_STATIC
        if (!validateRepoName) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("repoName"))
        }
        return repoName
    }

    fun parseArtifactoryType(repoName: String): ArtifactoryType {
        return if (repoName == REPO_NAME_CUSTOM) {
            ArtifactoryType.CUSTOM_DIR
        } else if (repoName == REPO_NAME_IMAGE) {
            ArtifactoryType.IMAGE
        } else {
            ArtifactoryType.PIPELINE
        }
    }

    fun getRepoName(artifactoryType: ArtifactoryType?): String {
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> REPO_NAME_PIPELINE
            ArtifactoryType.CUSTOM_DIR -> REPO_NAME_CUSTOM
            else -> BKREPO_COMMOM_REPO
        }
    }

    fun getRepoName(fileType: FileTypeEnum?): String {
        return when (fileType) {
            FileTypeEnum.BK_ARCHIVE -> REPO_NAME_PIPELINE
            FileTypeEnum.BK_CUSTOM -> REPO_NAME_CUSTOM
            FileTypeEnum.BK_REPORT -> REPO_NAME_REPORT
            FileTypeEnum.BK_STATIC -> REPO_NAME_STATIC
            else -> BKREPO_COMMOM_REPO
        }
    }

    fun parsePathNamePair(path: String): Pair<String, String> {
        val absPath = "/${path.removePrefix("/")}"
        return if (absPath.endsWith("/")) {
            Pair(absPath, "*")
        } else {
            val fileName = absPath.split("/").last()
            val filePath = absPath.removeSuffix(fileName)
            Pair(filePath, fileName)
        }
    }

    fun NodeDetail.toFileDetail(): FileDetail {
        return FileDetail(
            name = name,
            path = path,
            fullName = fullPath,
            fullPath = fullPath,
            size = size,
            createdTime = LocalDateTime.parse(createdDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            modifiedTime = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            checksums = FileChecksums(sha256, "", md5 ?: ""),
            meta = metadata.entries.associate { Pair(it.key, it.value.toString()) }
        )
    }

    fun NodeInfo.toFileInfo(): FileInfo {
        val properties = metadata?.map { Property(it.key, it.value.toString()) }
        return FileInfo(
            name = name,
            fullName = fullPath,
            path = fullPath, // bug?
            fullPath = fullPath,
            size = size,
            folder = folder,
            modifiedTime = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME)
                .timestamp(),
            artifactoryType = ArtifactoryType.CUSTOM_DIR,
            properties = properties,
            md5 = md5
        )
    }
}
