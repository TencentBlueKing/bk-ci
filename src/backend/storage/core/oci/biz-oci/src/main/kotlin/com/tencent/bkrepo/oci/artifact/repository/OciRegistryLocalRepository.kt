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

package com.tencent.bkrepo.oci.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.oci.config.OciProperties
import com.tencent.bkrepo.oci.constant.APP_VERSION
import com.tencent.bkrepo.oci.constant.BLOB_UNKNOWN_CODE
import com.tencent.bkrepo.oci.constant.BLOB_UNKNOWN_DESCRIPTION
import com.tencent.bkrepo.oci.constant.CHART_LAYER_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.DESCRIPTION
import com.tencent.bkrepo.oci.constant.FORCE
import com.tencent.bkrepo.oci.constant.MANIFEST_DIGEST
import com.tencent.bkrepo.oci.constant.MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.MEDIA_TYPE_ALL
import com.tencent.bkrepo.oci.exception.OciFileNotFoundException
import com.tencent.bkrepo.oci.model.Descriptor
import com.tencent.bkrepo.oci.model.ManifestSchema2
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.pojo.digest.OciDigest
import com.tencent.bkrepo.oci.service.OciOperationService
import com.tencent.bkrepo.oci.util.ObjectBuildUtils
import com.tencent.bkrepo.oci.util.OciLocationUtils
import com.tencent.bkrepo.oci.util.OciResponseUtils
import com.tencent.bkrepo.oci.util.OciUtils
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OciRegistryLocalRepository(
    private val ociProperties: OciProperties,
    private val ociOperationService: OciOperationService
) : LocalRepository() {

    /**
     * 上传前回调
     */
    override fun onUploadBefore(context: ArtifactUploadContext) {
        with(context) {
            super.onUploadBefore(context)
            val isForce = request.getParameter(FORCE)?.let { true } ?: false
            val projectId = repositoryDetail.projectId
            val repoName = repositoryDetail.name
            val fullPath = getFullPath(context)
            val isExist = nodeClient.checkExist(projectId, repoName, fullPath).data!!
            logger.info(
                "The file $fullPath that will be uploaded to server is exist: $isExist " +
                    "in repo ${artifactInfo.getRepoIdentify()}, and the flag of force overwrite is $isForce"
            )
            if (isExist && !isForce) {
                logger.warn(
                    "${fullPath.trimStart('/')} already exists in repo ${artifactInfo.getRepoIdentify()}"
                )
                return
            }
        }
    }

    /**
     * 上传
     */
    override fun onUpload(context: ArtifactUploadContext) {
        logger.info("Preparing to upload the oci file in repo ${context.artifactInfo.getRepoIdentify()}")
        val artifactFile = context.getArtifactFile()
        val digest = OciDigest.fromSha256(artifactFile.getFileSha256())
        val fullPath = storeArtifact(context)
        // 获取用户可访问地址
        val location = when (context.artifactInfo) {
            is OciManifestArtifactInfo -> {
                // 上传manifest文件，同时需要将manifest中对应blob的属性进行补充到blob节点中，同时创建package相关信息
                with(context.artifactInfo as OciManifestArtifactInfo) {
                    updateOciInfo(
                        ociArtifactInfo = this,
                        digest = digest,
                        artifactFile = artifactFile,
                        fullPath = fullPath,
                        context = context
                    )
                    OciLocationUtils.manifestLocation(digest, this)
                }
            }
            is OciBlobArtifactInfo -> {
                with(context.artifactInfo as OciBlobArtifactInfo) {
                    OciLocationUtils.blobLocation(digest, this)
                }
            }
            else -> null
        }
        logger.info(
            "Artifact $fullPath has been uploaded and will can be accessed in $location" +
                " in repo ${context.artifactInfo.getRepoIdentify()}"
        )
        location?.let {
            OciResponseUtils.buildUploadResponse(
                domain = ociProperties.domain,
                digest = digest,
                locationStr = location,
                response = context.response
            )
        }
    }

    /**
     * 更新整个blob相关信息,blob相关的mediatype，version等信息需要从manifest中获取
     */
    private fun updateOciInfo(
        ociArtifactInfo: OciManifestArtifactInfo,
        digest: OciDigest,
        artifactFile: ArtifactFile,
        fullPath: String,
        context: ArtifactUploadContext
    ) {
        logger.info(
            "Will start to update oci info for ${ociArtifactInfo.getArtifactFullPath()} " +
                "in repo ${ociArtifactInfo.getRepoIdentify()}"
        )
        val manifest = OciUtils.streamToManifest(artifactFile.getInputStream())
        // 更新manifest文件的metadata
        val mediaType = HeaderUtils.getHeader(HttpHeaders.CONTENT_TYPE).orEmpty()
        updateNodeMetaData(
            digest = digest.toString(),
            schemaVersion = manifest.schemaVersion,
            ociArtifactInfo = ociArtifactInfo,
            fullPath = fullPath,
            mediaType = mediaType
        )
        // 同步blob相关metadata
        syncBlobInfo(
            ociArtifactInfo = ociArtifactInfo,
            manifest = manifest,
            manifestDigest = digest,
            context = context,
            manifestPath = fullPath
        )
    }
    /**
     * 将部分信息存入节点metadata中
     */
    private fun updateNodeMetaData(
        digest: String,
        schemaVersion: Int? = null,
        ociArtifactInfo: OciManifestArtifactInfo,
        fullPath: String,
        mediaType: String,
        manifestDigest: String? = null,
        chartYaml: Map<String, Any>? = null
    ) {
        logger.info("The mediaType of $fullPath file that has been uploaded is $mediaType")
        // 将基础信息存储到metadata中
        val metadata = ObjectBuildUtils.buildMetadata(
            mediaType = mediaType,
            digest = digest,
            version = ociArtifactInfo.reference,
            schemaVersion = schemaVersion,
            manifestDigest = manifestDigest,
            yamlData = chartYaml
        )
        ociOperationService.saveMetaData(
            projectId = ociArtifactInfo.projectId,
            repoName = ociArtifactInfo.repoName,
            fullPath = fullPath,
            metadata = metadata
        )
    }

    /**
     * 构造节点创建请求
     */
    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        return ObjectBuildUtils.buildNodeCreateRequest(
            projectId = context.artifactInfo.projectId,
            repoName = context.artifactInfo.repoName,
            artifactFile = context.getArtifactFile(),
            fullPath = getFullPath(context)
        )
    }

    private fun getFullPath(context: ArtifactUploadContext): String {
        return when (context.artifactInfo) {
            is OciManifestArtifactInfo -> {
                with(context.artifactInfo as OciManifestArtifactInfo) {
                    val path = OciLocationUtils.buildDigestManifestPathWithSha256(
                        packageName,
                        context.getArtifactSha256()
                    )
                    logger.info("$packageName' manifest file will be stored in [$path] under repo ${getRepoIdentify()}")
                    path
                }
            }
            is OciBlobArtifactInfo -> {
                with(context.artifactInfo as OciBlobArtifactInfo) {
                    val path = getArtifactFullPath()
                    logger.info(
                        "$packageName blob file will be stored in [$path] under repo ${getRepoIdentify()}"
                    )
                    path
                }
            }
            else -> context.artifactInfo.getArtifactFullPath()
        }
    }

    /**
     * 同步blob层的数据和config里面的数据
     */
    private fun syncBlobInfo(
        ociArtifactInfo: OciManifestArtifactInfo,
        manifest: ManifestSchema2,
        manifestDigest: OciDigest,
        context: ArtifactUploadContext,
        manifestPath: String
    ) {
        logger.info(
            "Will start to sync blobs and config info from manifest ${ociArtifactInfo.getArtifactFullPath()} " +
                "to blobs in repo ${ociArtifactInfo.getRepoIdentify()}."
        )
        val descriptorList = OciUtils.manifestIterator(manifest)

        var chartYaml: Map<String, Any>? = null
        // 统计所有mainfest中的文件size作为整个package version的size
        var size: Long = 0
        // 同步layer以及config层blob信息
        descriptorList.forEach {
            size += it.size
            chartYaml = when (it.mediaType) {
                CHART_LAYER_MEDIA_TYPE -> {
                    // 针对helm chart，需要将chart.yaml中相关信息存入对应节点中
                    ociOperationService.loadArtifactInput(
                        chartDigest = it.digest,
                        projectId = ociArtifactInfo.projectId,
                        repoName = ociArtifactInfo.repoName,
                        packageName = ociArtifactInfo.packageName,
                        version = ociArtifactInfo.reference,
                        storageCredentials = context.storageCredentials
                    )
                }
                else -> null
            }
            doSyncBlob(it, ociArtifactInfo, manifestDigest, chartYaml)
        }
        // 根据flag生成package信息以及packageversion信息
        doPackageOperations(
            manifestPath = manifestPath,
            ociArtifactInfo = ociArtifactInfo,
            manifestDigest = manifestDigest,
            size = size,
            chartYaml = chartYaml
        )
    }

    /**
     * 更新blobs的信息
     */
    private fun doSyncBlob(
        descriptor: Descriptor,
        ociArtifactInfo: OciManifestArtifactInfo,
        manifestDigest: OciDigest,
        chartYaml: Map<String, Any>? = null
    ) {
        with(ociArtifactInfo) {
            logger.info(
                "Handling sync blob digest [${descriptor.digest}] in repo ${ociArtifactInfo.getRepoIdentify()}"
            )
            if (!OciDigest.isValid(descriptor.digest)) {
                logger.info("Invalid blob digest [$descriptor]")
                return
            }
            val blobDigest = OciDigest(descriptor.digest)
            val fullPath = OciLocationUtils.buildDigestBlobsPath(packageName, blobDigest)
            updateBlobMetaData(
                fullPath = fullPath,
                descriptor = descriptor,
                ociArtifactInfo = this,
                manifestDigest = manifestDigest,
                yamlMap = chartYaml
            )
        }
    }

    /**
     * 根据manifest文件中的信息更新blob metadata信息
     */
    private fun updateBlobMetaData(
        fullPath: String,
        descriptor: Descriptor,
        ociArtifactInfo: OciManifestArtifactInfo,
        manifestDigest: OciDigest,
        yamlMap: Map<String, Any>? = null
    ) {
        with(ociArtifactInfo) {
            nodeClient.getNodeDetail(projectId, repoName, fullPath).data?.let {
                logger.info(
                    "The current blob [${descriptor.digest}] is stored in $fullPath with package $packageName " +
                        "and version $reference under repo ${getRepoIdentify()}"
                )
                updateNodeMetaData(
                    digest = descriptor.digest,
                    ociArtifactInfo = this,
                    fullPath = it.fullPath,
                    mediaType = descriptor.mediaType,
                    manifestDigest = manifestDigest.toString(),
                    chartYaml = yamlMap
                )
            }
        }
    }

    /**
     * 根据blob信息生成对应的package以及version信息
     */
    private fun doPackageOperations(
        manifestPath: String,
        ociArtifactInfo: OciManifestArtifactInfo,
        manifestDigest: OciDigest,
        size: Long,
        chartYaml: Map<String, Any>? = null
    ) {
        with(ociArtifactInfo) {
            logger.info("Will create package info for [$packageName/$version in repo ${getRepoIdentify()} ")
            val packageVersion = packageClient.findVersionByName(
                projectId = projectId,
                repoName = repoName,
                packageKey = PackageKeys.ofOci(packageName),
                version = ociArtifactInfo.reference
            ).data
            val metadata = mutableMapOf<String, Any>(MANIFEST_DIGEST to manifestDigest.toString())
                .apply { chartYaml?.let { this.putAll(chartYaml) } }
            if (packageVersion == null) {
                val request = ObjectBuildUtils.buildPackageVersionCreateRequest(
                    ociArtifactInfo = this,
                    packageName = packageName,
                    version = ociArtifactInfo.reference,
                    size = size,
                    fullPath = manifestPath,
                    metadata = metadata
                )
                packageClient.createVersion(request)
            } else {
                val request = ObjectBuildUtils.buildPackageVersionUpdateRequest(
                    ociArtifactInfo = this,
                    packageName = packageName,
                    version = ociArtifactInfo.reference,
                    size = size,
                    fullPath = manifestPath,
                    metadata = metadata
                )
                packageClient.updateVersion(request)
            }

            // 针对helm chart包，将部分信息放入到package中
            var appVersion: String? = null
            var description: String? = null
            chartYaml?.let {
                appVersion = it[APP_VERSION] as String?
                description = it[DESCRIPTION] as String?
            }
            ociOperationService.updatePackageInfo(ociArtifactInfo, appVersion, description)
        }
    }

    /**
     * 保存文件内容
     */
    private fun storeArtifact(context: ArtifactUploadContext): String {
        val request = buildNodeCreateRequest(context)
        storageManager.storeArtifactFile(request, context.getArtifactFile(), context.storageCredentials)
        return request.fullPath
    }

    /**
     * 在原有逻辑上增加响应头
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        logger.info("Will start to download oci artifact in repo ${context.artifactInfo.getRepoIdentify()}...")
        // 根据类型解析实际存储路径，manifest有可能根据版本进行获取而不是对应文件的digest值
        val fullPath = when (context.artifactInfo) {
            is OciBlobArtifactInfo -> {
                with(context.artifactInfo as OciBlobArtifactInfo) {
                    getArtifactFullPath()
                }
            }
            is OciManifestArtifactInfo -> {
                with(context.artifactInfo as OciManifestArtifactInfo) {
                    if (isValidDigest) {
                        getArtifactFullPath()
                    } else {
                        ociOperationService.queryManifestFullPathByNameAndDigest(
                            projectId = projectId,
                            repoName = repoName,
                            version = reference,
                            packageName = packageName
                        )
                    }
                }
            }
            else -> null
        }
        return downloadArtifact(context, fullPath)
    }

    private fun downloadArtifact(context: ArtifactDownloadContext, fullPath: String?): ArtifactResource? {
        logger.info("Starting to download $fullPath in repo: ${context.artifactInfo.getRepoIdentify()}")
        if (fullPath.isNullOrBlank()) return null
        val node = nodeClient.getNodeDetail(context.projectId, context.repoName, fullPath).data
        val inputStream = storageManager.loadArtifactInputStream(node, context.storageCredentials)
            ?: throw OciFileNotFoundException(
                "Could not get artifact $fullPath file in repo: ${context.artifactInfo.getRepoIdentify()}",
                BLOB_UNKNOWN_CODE,
                BLOB_UNKNOWN_DESCRIPTION
            )
        val digest = OciDigest.fromSha256(node!!.sha256.orEmpty())
        val mediaType = if (node.metadata[MEDIA_TYPE] == null) {
            MEDIA_TYPE_ALL
        } else {
            node.metadata[MEDIA_TYPE] as String
        }
        logger.info(
            "The mediaType of Artifact $fullPath is $mediaType in repo: ${context.artifactInfo.getRepoIdentify()}"
        )
        OciResponseUtils.buildDownloadResponse(digest, context.response, node.size)
        val resource = ArtifactResource(
            inputStream = inputStream,
            artifactName = context.artifactInfo.getResponseName(),
            node = node,
            channel = ArtifactChannel.LOCAL,
            useDisposition = context.useDisposition
        )
        resource.contentType = mediaType
        return resource
    }

    /**
     * 版本不存在时 status code 404
     */
    override fun remove(context: ArtifactRemoveContext) {
        with(context.artifactInfo) {
            nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
                ?: throw OciFileNotFoundException(
                    "node [${getArtifactFullPath()}] in repo ${getRepoIdentify()} does not found."
                )
            logger.info("Ready to delete ${getArtifactFullPath()} in repo ${getRepoIdentify()}")
            val request = NodeDeleteRequest(projectId, repoName, getArtifactFullPath(), context.userId)
            nodeClient.deleteNode(request)
            OciResponseUtils.buildDeleteResponse(context.response)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciRegistryLocalRepository::class.java)
    }
}
