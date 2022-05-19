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

package com.tencent.bkrepo.oci.service

import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.oci.pojo.OciDomainInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.user.PackageVersionInfo

interface OciOperationService {

    /**
     * 保存节点元数据
     */
    fun saveMetaData(
        projectId: String,
        repoName: String,
        fullPath: String,
        metadata: MutableMap<String, Any>
    )

    /**
     * 当mediatype为CHART_LAYER_MEDIA_TYPE，需要解析chart.yaml文件
     */
    fun loadArtifactInput(
        chartDigest: String?,
        projectId: String,
        repoName: String,
        packageName: String,
        version: String,
        storageCredentials: StorageCredentials?
    ): Map<String, Any>?

    /**
     * 需要将blob中相关metadata写进package version中
     */
    fun updatePackageInfo(
        ociArtifactInfo: OciArtifactInfo,
        appVersion: String? = null,
        description: String? = null
    )

    /**
     * 根据artifactInfo获取对应manifest文件path
     */
    fun queryManifestFullPathByNameAndDigest(
        projectId: String,
        repoName: String,
        packageName: String,
        version: String
    ): String

    /**
     * 查询包版本详情
     */
    fun detailVersion(
        userId: String,
        artifactInfo: OciArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo

    /**
     * 删除包
     */
    fun deletePackage(userId: String, artifactInfo: OciArtifactInfo)

    /**
     * 删除包对应版本
     */
    fun deleteVersion(userId: String, artifactInfo: OciArtifactInfo)

    /**
     * 获取对应域名
     */
    fun getRegistryDomain(): OciDomainInfo
}
