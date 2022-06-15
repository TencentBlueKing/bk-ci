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

package com.tencent.bkrepo.oci.util

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.oci.constant.IMAGE_VERSION
import com.tencent.bkrepo.oci.constant.MEDIA_TYPE
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.pojo.user.BasicInfo
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest

object ObjectBuildUtils {

    fun buildNodeCreateRequest(
        projectId: String,
        repoName: String,
        artifactFile: ArtifactFile,
        fullPath: String,
        metadata: Map<String, Any>? = null
    ): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = projectId,
            repoName = repoName,
            folder = false,
            fullPath = fullPath,
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            operator = SecurityUtils.getUserId(),
            overwrite = true,
            metadata = metadata
        )
    }

    fun buildMetadata(
        mediaType: String,
        version: String?,
        yamlData: Map<String, Any>? = null
    ): MutableMap<String, Any> {
        return mutableMapOf<String, Any>(
            MEDIA_TYPE to mediaType
        ).apply {
            version?.let { this.put(IMAGE_VERSION, version) }
            yamlData?.let {
                this.putAll(yamlData)
            }
        }
    }

    fun buildMetadataSaveRequest(
        projectId: String,
        repoName: String,
        fullPath: String,
        metadata: Map<String, Any>? = null
    ): MetadataSaveRequest {
        return MetadataSaveRequest(
            projectId = projectId,
            repoName = repoName,
            fullPath = fullPath,
            metadata = metadata,
            operator = SecurityUtils.getUserId()
        )
    }

    fun buildPackageVersionCreateRequest(
        ociArtifactInfo: OciManifestArtifactInfo,
        packageName: String,
        version: String,
        size: Long,
        fullPath: String,
        repoType: String,
        metadata: Map<String, Any>? = null
    ): PackageVersionCreateRequest {
        with(ociArtifactInfo) {
            // 兼容多仓库类型支持
            val packageType = PackageType.valueOf(repoType)
            val packageKey = PackageKeys.ofName(repoType.toLowerCase(), packageName)
            return PackageVersionCreateRequest(
                projectId = projectId,
                repoName = repoName,
                packageName = packageName,
                packageKey = packageKey,
                packageType = packageType,
                versionName = version,
                size = size,
                artifactPath = fullPath,
                metadata = metadata,
                overwrite = true,
                createdBy = SecurityUtils.getUserId()
            )
        }
    }

    fun buildPackageVersionUpdateRequest(
        ociArtifactInfo: OciManifestArtifactInfo,
        version: String,
        size: Long,
        fullPath: String,
        metadata: Map<String, Any>? = null,
        packageKey: String
    ): PackageVersionUpdateRequest {
        with(ociArtifactInfo) {
            return PackageVersionUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                versionName = version,
                size = size,
                manifestPath = fullPath,
                metadata = metadata
            )
        }
    }

    fun buildPackageUpdateRequest(
        artifactInfo: ArtifactInfo,
        name: String,
        packageKey: String,
        appVersion: String? = null,
        description: String? = null
    ): PackageUpdateRequest {
        return PackageUpdateRequest(
            projectId = artifactInfo.projectId,
            repoName = artifactInfo.repoName,
            name = name,
            description = description,
            packageKey = packageKey,
            extension = appVersion?.let { mapOf("appVersion" to appVersion) }
        )
    }

    fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
        with(nodeDetail) {
            return BasicInfo(
                version = packageVersion.name,
                fullPath = fullPath,
                size = size,
                sha256 = sha256.orEmpty(),
                md5 = md5.orEmpty(),
                stageTag = packageVersion.stageTag,
                projectId = projectId,
                repoName = repoName,
                downloadCount = packageVersion.downloads,
                createdBy = createdBy,
                createdDate = createdDate,
                lastModifiedBy = lastModifiedBy,
                lastModifiedDate = lastModifiedDate
            )
        }
    }
}
