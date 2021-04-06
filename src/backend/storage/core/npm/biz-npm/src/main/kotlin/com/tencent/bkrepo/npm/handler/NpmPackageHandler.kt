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

package com.tencent.bkrepo.npm.handler

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_PKG_TGZ_FULL_PATH
import com.tencent.bkrepo.npm.constants.SIZE
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.model.properties.PackageProperties
import com.tencent.bkrepo.npm.pojo.fixtool.FailPackageDetail
import com.tencent.bkrepo.npm.pojo.fixtool.FailVersionDetail
import com.tencent.bkrepo.npm.utils.BeanUtils
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PopulatedPackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NpmPackageHandler {
    @Autowired
    private lateinit var packageClient: PackageClient

    /**
     * 包版本数据填充
     * [nodeInfo] package.json的node节点信息
     */
    fun populatePackage(
        nodeInfo: NodeInfo,
        packageMetaData: NpmPackageMetaData,
        tgzNodeInfoMap: Map<String, NodeInfo>
    ): FailPackageDetail? {
        val versionList = mutableListOf<PopulatedPackageVersion>()
        with(packageMetaData) {
            val name = this.name.orEmpty()
            val iterator = packageMetaData.versions.map.entries.iterator()
            val failPackageDetail = FailPackageDetail(name, mutableSetOf())
            while (iterator.hasNext()) {
                val next = iterator.next()
                val version = next.key
                // tgz包不存在，补全包版本数据失败
                if (!tgzNodeInfoMap.containsKey(version)) {
                    val message = "the version [$version] for package [$name] with tgz file not found " +
                        "in repo [${nodeInfo.projectId}/${nodeInfo.repoName}]."
                    logger.warn(message)
                    val failVersionDetail = FailVersionDetail(version, message)
                    failPackageDetail.failedVersionSet.add(failVersionDetail)
                    continue
                }
                val tgzNodeInfo = tgzNodeInfoMap.getValue(version)
                val dist = next.value.dist!!
                val size = if (!dist.any().containsKey(SIZE)) {
                    tgzNodeInfoMap[next.key]?.size!!
                } else {
                    dist.any()[SIZE].toString().toLong()
                }
                with(tgzNodeInfo) {
                    val populatedPackageVersion = PopulatedPackageVersion(
                        createdBy = createdBy,
                        createdDate = LocalDateTime.parse(createdDate),
                        lastModifiedBy = lastModifiedBy,
                        lastModifiedDate = LocalDateTime.parse(lastModifiedDate),
                        name = version,
                        size = size,
                        downloads = 0,
                        manifestPath = getManifestPath(name, version),
                        artifactPath = fullPath,
                        metadata = buildProperties(next.value)
                    )
                    versionList.add(populatedPackageVersion)
                }
            }
            with(nodeInfo) {
                val packagePopulateRequest = PackagePopulateRequest(
                    createdBy = nodeInfo.createdBy,
                    createdDate = LocalDateTime.parse(createdDate),
                    lastModifiedBy = nodeInfo.lastModifiedBy,
                    lastModifiedDate = LocalDateTime.parse(lastModifiedDate),
                    projectId = nodeInfo.projectId,
                    repoName = nodeInfo.repoName,
                    name = name,
                    key = PackageKeys.ofNpm(name),
                    type = PackageType.NPM,
                    description = packageMetaData.description.orEmpty(),
                    versionList = versionList
                )
                packageClient.populatePackage(packagePopulateRequest)
            }
            return if (failPackageDetail.failedVersionSet.isEmpty()) null else failPackageDetail
        }
    }

    /**
     * 创建包版本
     */
    fun createVersion(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        versionMetaData: NpmVersionMetadata,
        size: Long
    ) {
        versionMetaData.apply {
            val name = this.name!!
            val description = this.description
            val version = this.version!!
            val manifestPath = getManifestPath(name, version)
            val contentPath = getContentPath(name, version)
            val metadata = buildProperties(this)
            with(artifactInfo) {
                val packageVersionCreateRequest = PackageVersionCreateRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageName = name,
                    packageKey = PackageKeys.ofNpm(name),
                    packageType = PackageType.NPM,
                    packageDescription = description,
                    versionName = version,
                    size = size,
                    manifestPath = manifestPath,
                    artifactPath = contentPath,
                    stageTag = null,
                    metadata = metadata,
                    overwrite = true,
                    createdBy = userId
                )
                packageClient.createVersion(packageVersionCreateRequest).apply {
                    logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
                }
            }
        }
    }

    private fun buildProperties(npmVersionMetadata: NpmVersionMetadata?): Map<String, String> {
        return npmVersionMetadata?.let {
            val value = JsonUtils.objectMapper.writeValueAsString(it)
            val npmProperties = JsonUtils.objectMapper.readValue(value, PackageProperties::class.java)
            BeanUtils.beanToMap(npmProperties)
        } ?: emptyMap()
    }

    /**
     * 删除包
     */
    fun deletePackage(userId: String, name: String, artifactInfo: NpmArtifactInfo) {
        val packageKey = PackageKeys.ofNpm(name)
        with(artifactInfo) {
            packageClient.deletePackage(projectId, repoName, packageKey).apply {
                logger.info("user: [$userId] delete package [$name] in repo [$projectId/$repoName] success!")
            }
        }
    }

    /**
     * 删除版本
     */
    fun deleteVersion(userId: String, name: String, version: String, artifactInfo: NpmArtifactInfo) {
        val packageKey = PackageKeys.ofNpm(name)
        with(artifactInfo) {
            packageClient.deleteVersion(projectId, repoName, packageKey, version).apply {
                logger.info(
                    "user: [$userId] delete package [$name] with version [$version] " +
                        "in repo [$projectId/$repoName] success!"
                )
            }
        }
    }

    fun getManifestPath(name: String, version: String): String {
        return NpmUtils.getVersionPackageMetadataPath(name, version)
    }

    fun getContentPath(name: String, version: String): String {
        return String.format(NPM_PKG_TGZ_FULL_PATH, name, name, version)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NpmPackageHandler::class.java)
    }
}
