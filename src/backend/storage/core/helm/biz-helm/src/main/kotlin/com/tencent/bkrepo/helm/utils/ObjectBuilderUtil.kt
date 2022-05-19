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

import com.tencent.bkrepo.common.api.util.toYamlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.META_DETAIL
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.pojo.chart.ChartOperationRequest
import com.tencent.bkrepo.helm.pojo.chart.ChartUploadRequest
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.pojo.user.BasicInfo
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest

object ObjectBuilderUtil {

    fun buildPackageUpdateRequest(
        projectId: String,
        repoName: String,
        chartInfo: HelmChartMetadata
    ): PackageUpdateRequest {
        return buildPackageUpdateRequest(
            projectId = projectId,
            repoName = repoName,
            name = chartInfo.name,
            appVersion = chartInfo.appVersion.toString(),
            description = chartInfo.description
        )
    }

    fun buildPackageUpdateRequest(
        projectId: String,
        repoName: String,
        name: String,
        appVersion: String,
        description: String?
    ): PackageUpdateRequest {
        return PackageUpdateRequest(
            projectId = projectId,
            repoName = repoName,
            name = name,
            packageKey = PackageKeys.ofHelm(name),
            description = description,
            versionTag = null,
            extension = mapOf("appVersion" to appVersion)
        )
    }

    fun buildPackageVersionCreateRequest(
        userId: String,
        projectId: String,
        repoName: String,
        chartInfo: HelmChartMetadata,
        size: Long,
        isOverwrite: Boolean = false
    ): PackageVersionCreateRequest {
        return PackageVersionCreateRequest(
            projectId = projectId,
            repoName = repoName,
            packageName = chartInfo.name,
            packageKey = PackageKeys.ofHelm(chartInfo.name),
            packageType = PackageType.HELM,
            packageDescription = chartInfo.description,
            versionName = chartInfo.version,
            size = size,
            manifestPath = null,
            artifactPath = HelmUtils.getChartFileFullPath(chartInfo.name, chartInfo.version),
            stageTag = null,
            metadata = HelmMetadataUtils.convertToMap(chartInfo),
            overwrite = isOverwrite,
            createdBy = userId
        )
    }

    fun buildDownloadRecordRequest(
        context: ArtifactDownloadContext
    ): PackageDownloadRecord? {
        val name = context.getStringAttribute(NAME).orEmpty()
        val version = context.getStringAttribute(VERSION).orEmpty()
        // 下载index.yaml不进行下载次数统计
        if (name.isEmpty() && version.isEmpty()) return null
        with(context) {
            return PackageDownloadRecord(projectId, repoName, PackageKeys.ofHelm(name), version)
        }
    }
    fun buildIndexYamlRequest(): ArtifactInfo {
        val artifactInfo = HttpContextHolder.getRequest().getAttribute(ARTIFACT_INFO_KEY) as ArtifactInfo
        return buildIndexYamlRequest(artifactInfo)
    }

    fun buildIndexYamlRequest(projectId: String, repoName: String): ArtifactInfo {
        val artifactInfo = ArtifactInfo(projectId, repoName, "/")
        return buildIndexYamlRequest(artifactInfo)
    }

    fun buildIndexYamlRequest(artifactInfo: ArtifactInfo): ArtifactInfo {
        val path = HelmUtils.getIndexCacheYamlFullPath()
        return ArtifactInfo(artifactInfo.projectId, artifactInfo.repoName, path)
    }

    fun buildFileAndNodeCreateRequest(
        indexYamlMetadata: HelmIndexYamlMetadata,
        request: ChartOperationRequest
    ): Pair<ArtifactFile, NodeCreateRequest> {
        return buildFileAndNodeCreateRequest(
            indexYamlMetadata = indexYamlMetadata,
            projectId = request.projectId,
            repoName = request.repoName,
            operator = request.operator
        )
    }

    fun buildFileAndNodeCreateRequest(
        indexYamlMetadata: HelmIndexYamlMetadata,
        projectId: String,
        repoName: String,
        operator: String
    ): Pair<ArtifactFile, NodeCreateRequest> {
        val artifactFile = ArtifactFileFactory.build(indexYamlMetadata.toYamlString().byteInputStream())
        val nodeCreateRequest = NodeCreateRequest(
            projectId = projectId,
            repoName = repoName,
            folder = false,
            fullPath = HelmUtils.getIndexCacheYamlFullPath(),
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            overwrite = true,
            operator = operator
        )
        return Pair(artifactFile, nodeCreateRequest)
    }

    fun buildChartUploadRequest(
        context: ArtifactUploadContext
    ): ChartUploadRequest {
        return ChartUploadRequest(
            projectId = context.projectId,
            repoName = context.repoName,
            name = context.getStringAttribute(NAME)!!,
            version = context.getStringAttribute(VERSION)!!,
            operator = context.userId,
            fullPath = context.getStringAttribute(FULL_PATH)!!,
            metadataMap = context.getAttribute(META_DETAIL),
            artifactInfo = context.artifactInfo
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
