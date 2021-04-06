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

package com.tencent.bkrepo.maven.artifact.repository

import com.tencent.bkrepo.common.api.util.readXmlString
import com.tencent.bkrepo.common.api.util.toXmlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.pojo.Basic
import com.tencent.bkrepo.maven.pojo.MavenArtifactVersionData
import com.tencent.bkrepo.maven.pojo.MavenMetadata
import com.tencent.bkrepo.maven.pojo.MavenPom
import com.tencent.bkrepo.maven.pojo.MavenGAVC
import com.tencent.bkrepo.maven.util.MavenGAVCUtils.mavenGAVC
import com.tencent.bkrepo.maven.util.StringUtils.formatSeparator
import com.tencent.bkrepo.repository.api.StageClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.util.regex.Pattern

@Component
class MavenLocalRepository(private val stageClient: StageClient) : LocalRepository() {

    /**
     * 获取MAVEN节点创建请求
     */
    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        return request.copy(overwrite = true)
    }

    private fun buildMavenArtifactNode(context: ArtifactUploadContext, packaging: String): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        return request.copy(
            overwrite = true,
            metadata = mapOf("packaging" to packaging)
        )
    }

    override fun onUpload(context: ArtifactUploadContext) {
        val regex = "(.)+-(.)+\\.(jar|war|tar|ear|ejb|rar|msi|rpm|tar\\.bz2|tar\\.gz|tbz|zip|pom)$"
        val matcher = Pattern.compile(regex).matcher(context.artifactInfo.getArtifactFullPath())
        if (matcher.find()) {
            val packaging = matcher.group(3)
            if (packaging == "pom") {
                val mavenPom = context.getArtifactFile().getInputStream().readXmlString<MavenPom>()
                if (StringUtils.isNotBlank(mavenPom.version) && mavenPom.packaging == "pom") {
                    val node = buildMavenArtifactNode(context, packaging)
                    storageManager.storeArtifactFile(node, context.getArtifactFile(), context.storageCredentials)
                    createMavenVersion(context, mavenPom)
                } else {
                    super.onUpload(context)
                }
            } else {
                val node = buildMavenArtifactNode(context, packaging)
                storageManager.storeArtifactFile(node, context.getArtifactFile(), context.storageCredentials)
                val mavenJar = (context.artifactInfo as MavenArtifactInfo).toMavenJar()
                createMavenVersion(context, mavenJar)
            }
        } else {
            super.onUpload(context)
        }
    }

    private fun createMavenVersion(context: ArtifactUploadContext, mavenGAVC: MavenGAVC) {
        packageClient.createVersion(
            PackageVersionCreateRequest(
                context.projectId,
                context.repoName,
                packageName = mavenGAVC.artifactId,
                packageKey = PackageKeys.ofGav(mavenGAVC.groupId, mavenGAVC.artifactId),
                packageType = PackageType.MAVEN,
                versionName = mavenGAVC.version,
                size = context.getArtifactFile().getSize(),
                artifactPath = context.artifactInfo.getArtifactFullPath(),
                overwrite = true,
                createdBy = context.userId
            )
        )
    }

    fun metadataNodeCreateRequest(
        context: ArtifactUploadContext,
        fullPath: String,
        metadataArtifact: ArtifactFile
    ): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        return request.copy(
            overwrite = true,
            fullPath = fullPath
        )
    }

    fun updateMetadata(fullPath: String, metadataArtifact: ArtifactFile) {
        val uploadContext = ArtifactUploadContext(metadataArtifact)
        val metadataNode = metadataNodeCreateRequest(uploadContext, fullPath, metadataArtifact)
        storageManager.storeArtifactFile(metadataNode, metadataArtifact, uploadContext.storageCredentials)
        logger.info("Success to save $fullPath, size: ${metadataArtifact.getSize()}")
    }

    override fun remove(context: ArtifactRemoveContext) {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        val artifactId = packageKey.split(":").last()
        val groupId = packageKey.removePrefix("gav://").split(":")[0]
        with(context.artifactInfo) {
            if (version.isNullOrBlank()) {
                packageClient.deletePackage(
                    projectId,
                    repoName,
                    packageKey
                )
            } else {
                packageClient.deleteVersion(
                    projectId,
                    repoName,
                    packageKey,
                    version
                )
            }
            logger.info("Success to delete $packageKey:$version")
        }
        updateMetadataXml(context, groupId, artifactId, version)
    }

    /**
     * 删除jar包时 对包一级目录下maven-metadata.xml 更新
     */
    fun updateMetadataXml(context: ArtifactRemoveContext, groupId: String, artifactId: String, version: String?) {
        val packageKey = context.request.getParameter("packageKey")
        val artifactPath = StringUtils.join(groupId.split("."), "/") + "/$artifactId"
        if (version.isNullOrBlank()) {
            nodeClient.deleteNode(
                NodeDeleteRequest(
                    context.projectId,
                    context.repoName,
                    artifactPath,
                    ArtifactRemoveContext().userId
                )
            )
            return
        }
        // 加载xml
        with(context.artifactInfo) {
            val nodeList = nodeClient.listNode(projectId, repoName, "/$artifactPath").data ?: return
            val mavenMetadataNode = nodeList.filter { it.name == "maven-metadata.xml" }[0]
            val artifactInputStream = storageService.load(
                mavenMetadataNode.sha256!!,
                Range.full(mavenMetadataNode.size),
                ArtifactRemoveContext().storageCredentials
            ) ?: return
            val xmlStr = String(artifactInputStream.readBytes())
                .removePrefix("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            val mavenMetadata = xmlStr.readXmlString<MavenMetadata>()
            mavenMetadata.versioning.versions.version.removeIf { it == version }
            // 当删除当前版本后不存在任一版本则删除整个包。
            if (mavenMetadata.versioning.versions.version.size == 0) {
                nodeClient.deleteNode(
                    NodeDeleteRequest(
                        projectId,
                        repoName,
                        artifactPath,
                        ArtifactRemoveContext().userId
                    )
                )
                packageClient.deletePackage(
                    projectId,
                    repoName,
                    packageKey
                )
                return
            } else {
                nodeClient.deleteNode(
                    NodeDeleteRequest(
                        projectId,
                        repoName,
                        "$artifactPath/$version",
                        ArtifactRemoveContext().userId
                    )
                )
                mavenMetadata.versioning.release = mavenMetadata.versioning.versions.version.last()
                val resultXml = mavenMetadata.toXmlString()
                val resultXmlMd5 = resultXml.md5()
                val resultXmlSha1 = resultXml.sha1()

                val metadataArtifact = ByteArrayInputStream(resultXml.toByteArray()).use {
                    ArtifactFileFactory.build(it)
                }
                val metadataArtifactMd5 = ByteArrayInputStream(resultXmlMd5.toByteArray()).use {
                    ArtifactFileFactory.build(it)
                }
                val metadataArtifactSha1 = ByteArrayInputStream(resultXmlSha1.toByteArray()).use {
                    ArtifactFileFactory.build(it)
                }

                logger.warn("${metadataArtifact.getSize()}")
                updateMetadata("$artifactPath/maven-metadata.xml", metadataArtifact)
                metadataArtifact.delete()
                updateMetadata("$artifactPath/maven-metadata.xml.md5", metadataArtifactMd5)
                metadataArtifactMd5.delete()
                updateMetadata("$artifactPath/maven-metadata.xml.sha1", metadataArtifactSha1)
                metadataArtifactSha1.delete()
            }
        }
    }

    override fun query(context: ArtifactQueryContext): MavenArtifactVersionData? {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        val artifactId = packageKey.split(":").last()
        val groupId = packageKey.removePrefix("gav://").split(":")[0]
        val trueVersion = packageClient.findVersionByName(
            context.projectId,
            context.repoName,
            packageKey,
            version
        ).data ?: return null
        with(context.artifactInfo) {
            val jarNode = nodeClient.getNodeDetail(
                projectId, repoName, trueVersion.contentPath!!
            ).data ?: return null
            val stageTag = stageClient.query(projectId, repoName, packageKey, version).data
            val mavenArtifactMetadata = jarNode.metadata
            val packageVersion = packageClient.findVersionByName(
                projectId, repoName, packageKey, version
            ).data
            val count = packageVersion?.downloads ?: 0
            val mavenArtifactBasic = Basic(
                groupId,
                artifactId,
                version,
                jarNode.size, jarNode.fullPath,
                jarNode.createdBy, jarNode.createdDate,
                jarNode.lastModifiedBy, jarNode.lastModifiedDate,
                count,
                jarNode.sha256,
                jarNode.md5,
                stageTag,
                null
            )
            return MavenArtifactVersionData(mavenArtifactBasic, mavenArtifactMetadata)
        }
    }

    // maven 客户端下载统计
    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context) {
            val fullPath = artifactInfo.getArtifactFullPath()
            val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
            return if (node != null && node.metadata["packaging"] != null) {
                val mavenGAVC = fullPath.mavenGAVC()
                val version = mavenGAVC.version
                val artifactId = mavenGAVC.artifactId
                val groupId = mavenGAVC.groupId.formatSeparator("/", ".")
                val packageKey = PackageKeys.ofGav(groupId, artifactId)
                PackageDownloadRecord(projectId, repoName, packageKey, version)
            } else {
                null
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenLocalRepository::class.java)
    }
}
