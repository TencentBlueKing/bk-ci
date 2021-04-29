/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.npm.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.npm.constant.KEYWORDS
import com.tencent.bkrepo.npm.constant.LATEST
import com.tencent.bkrepo.npm.constant.MAINTAINERS
import com.tencent.bkrepo.npm.constant.NAME
import com.tencent.bkrepo.npm.constant.NpmMessageCode
import com.tencent.bkrepo.npm.constant.NpmProperties
import com.tencent.bkrepo.npm.constant.PACKAGE
import com.tencent.bkrepo.npm.constant.SHA_SUM
import com.tencent.bkrepo.npm.pojo.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmPublishInfo
import com.tencent.bkrepo.npm.pojo.response.NpmResponse
import com.tencent.bkrepo.npm.util.NpmUtils
import com.tencent.bkrepo.repository.api.PackageDependentsClient
import com.tencent.bkrepo.repository.pojo.dependent.PackageDependentsRelation
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NpmRegistryLocalRepository(
    private val npmProperties: NpmProperties,
    private val packageDependentsClient: PackageDependentsClient
) : LocalRepository() {

    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        with(context.artifactInfo as NpmPublishInfo) {
            logger.info(
                "User[${context.userId}] prepare to publish package version [$packageName-$version] " +
                    "on ${getRepoIdentify()}, distTags: ${packageMetadata.distTags}"
            )
            // 验证checksum
            getVersionPackage().dist.shaSum?.let { context.validateDigest(it) }
            // 验证版本是否存在，存在则冲突
            packageClient.findVersionByName(projectId, repoName, packageName, version).data?.let {
                throw ErrorCodeException(NpmMessageCode.VERSION_EXITED, version, HttpStatus.FORBIDDEN)
            }
        }
    }

    override fun onUpload(context: ArtifactUploadContext) {
        uploadTarball(context)
        createVersion(context)
        context.response.status = HttpStatus.CREATED.value
        context.response.writer.write(NpmResponse.success().toJsonString())
    }

    /**
     * 在原有逻辑上添加了兼容处理
     * 如果/-/下载不到，则替换成/download/下载
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        return super.onDownload(context) ?: run {
            with(context) {
                val compatiblePath = NpmUtils.getCompatiblePath(artifactInfo.getArtifactFullPath())
                val node = nodeClient.getNodeDetail(projectId, repoName, compatiblePath).data
                val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials) ?: return null
                val responseName = artifactInfo.getResponseName()
                return ArtifactResource(inputStream, responseName, node, ArtifactChannel.LOCAL, useDisposition)
            }
        }
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context.artifactInfo as NpmArtifactInfo) {
            return PackageDownloadRecord(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                packageVersion = version
            )
        }
    }

    override fun remove(context: ArtifactRemoveContext) {
        with(context.artifactInfo as NpmArtifactInfo) {
            if (version.isNotBlank()) {
                packageClient.findVersionByName(projectId, repoName, packageName, version).data?.let {
                    removeVersion(this, it, context.userId)
                }
            } else {
                packageClient.listAllVersion(projectId, repoName, packageName).data.orEmpty().forEach {
                    removeVersion(this, it, context.userId)
                }
            }
        }
    }

    /**
     * 删除[version]
     * 对应的tarball节点也会一起删除
     */
    private fun removeVersion(artifactInfo: NpmArtifactInfo, version: PackageVersion, userId: String) {
        with(artifactInfo) {
            packageClient.deleteVersion(projectId, repoName, packageName, version.name)
            val tarballPath = version.contentPath.orEmpty()
            if (tarballPath.isNotBlank()) {
                val request = NodeDeleteRequest(projectId, repoName, tarballPath, userId)
                nodeClient.deleteNode(request)
            }
        }
    }

    /**
     * 保存tarball 文件内容
     */
    private fun uploadTarball(context: ArtifactUploadContext) {
        val request = buildNodeCreateRequest(context).copy(overwrite = true)
        storageManager.storeArtifactFile(request, context.getArtifactFile(), context.storageCredentials)
    }

    /**
     * 创建版本
     */
    @Suppress("UNCHECKED_CAST")
    private fun createVersion(context: ArtifactUploadContext) {
        with(context.artifactInfo as NpmPublishInfo) {
            val versionTag = packageMetadata.distTags.toMutableMap()
            val packageInfo = packageClient.findPackageByKey(projectId, repoName, packageName).data
            // 上传beta版本时，latest可能为空
            if (versionTag[LATEST] == null) {
                // 服务器不存在latest时，以当前版本为latest
                val latestVersion = packageClient.findVersionNameByTag(projectId, repoName, packageName, LATEST).data
                if (latestVersion == null) {
                    versionTag[LATEST] = version
                }
            }
            val versionPackage = getVersionPackage()
            val packageExtension = packageInfo?.extension.orEmpty().toMutableMap()
            // 更新maintainers
            val maintainers = NpmUtils.resolveMaintainers(packageExtension)
            val maintainerMap = maintainers.associateBy { it[NAME] }
            versionPackage.maintainers?.forEach {
                if (it[NAME] != null && !maintainerMap.containsKey(it[NAME])) {
                    maintainers.add(it)
                }
            }
            packageExtension[MAINTAINERS] = maintainers
            // versionExtension
            val versionExtension = mutableMapOf<String, Any>(
                PACKAGE to versionPackage.toJsonString()
            )
            val keywords = when (versionPackage.keywords) {
                is String -> listOf(versionPackage.keywords)
                is List<*> -> (versionPackage.keywords as List<*>).map { it.toString() }.toList()
                else -> emptyList()
            }
            val metadata = mutableMapOf(
                KEYWORDS to keywords,
                SHA_SUM to getVersionPackage().dist.shaSum.orEmpty()
            )
            val request = PackageVersionCreateRequest(
                projectId = projectId,
                repoName = repoName,
                packageName = packageName,
                packageKey = packageName,
                packageType = PackageType.NPM,
                packageExtension = packageExtension,
                versionTag = versionTag,
                versionName = version,
                size = getAttachment().length.toLong(),
                artifactPath = getArtifactFullPath(),
                metadata = metadata,
                extension = versionExtension,
                overwrite = true,
                createdBy = context.userId
            )
            packageClient.createVersion(request)
            addDependentsRelations(this)
        }
    }

    private fun addDependentsRelations(npmPublishInfo: NpmPublishInfo) {
        with(npmPublishInfo) {
            val relation = PackageDependentsRelation(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                dependencies = npmPublishInfo.getVersionPackage().dependencies?.keys.orEmpty()
            )
            packageDependentsClient.addDependents(relation)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmRegistryLocalRepository::class.java)
    }
}
