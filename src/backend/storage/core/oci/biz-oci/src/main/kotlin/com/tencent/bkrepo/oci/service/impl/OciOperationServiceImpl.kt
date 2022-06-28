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

package com.tencent.bkrepo.oci.service.impl

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.pojo.FileInfo
import com.tencent.bkrepo.oci.config.OciProperties
import com.tencent.bkrepo.oci.constant.APP_VERSION
import com.tencent.bkrepo.oci.constant.CHART_LAYER_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.DESCRIPTION
import com.tencent.bkrepo.oci.constant.DOCKER_IMAGE_MANIFEST_MEDIA_TYPE_V1
import com.tencent.bkrepo.oci.constant.DOWNLOADS
import com.tencent.bkrepo.oci.constant.LAST_MODIFIED_BY
import com.tencent.bkrepo.oci.constant.LAST_MODIFIED_DATE
import com.tencent.bkrepo.oci.constant.MANIFEST_DIGEST
import com.tencent.bkrepo.oci.constant.MANIFEST_UNKNOWN_CODE
import com.tencent.bkrepo.oci.constant.MANIFEST_UNKNOWN_DESCRIPTION
import com.tencent.bkrepo.oci.constant.NODE_FULL_PATH
import com.tencent.bkrepo.oci.constant.OCI_IMAGE_MANIFEST_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.OCI_NODE_FULL_PATH
import com.tencent.bkrepo.oci.constant.OCI_PACKAGE_NAME
import com.tencent.bkrepo.oci.constant.PROXY_URL
import com.tencent.bkrepo.oci.constant.REPO_TYPE
import com.tencent.bkrepo.oci.exception.OciBadRequestException
import com.tencent.bkrepo.oci.exception.OciFileNotFoundException
import com.tencent.bkrepo.oci.model.Descriptor
import com.tencent.bkrepo.oci.model.ManifestSchema2
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.pojo.digest.OciDigest
import com.tencent.bkrepo.oci.pojo.response.OciImage
import com.tencent.bkrepo.oci.pojo.response.OciImageResult
import com.tencent.bkrepo.oci.pojo.response.OciTag
import com.tencent.bkrepo.oci.pojo.response.OciTagResult
import com.tencent.bkrepo.oci.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.oci.service.OciOperationService
import com.tencent.bkrepo.oci.util.ObjectBuildUtils
import com.tencent.bkrepo.oci.util.OciLocationUtils
import com.tencent.bkrepo.oci.util.OciResponseUtils
import com.tencent.bkrepo.oci.util.OciUtils
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import com.tencent.bkrepo.repository.pojo.search.PackageQueryBuilder
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.servlet.http.HttpServletRequest
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OciOperationServiceImpl(
    private val nodeClient: NodeClient,
    private val metadataClient: MetadataClient,
    private val packageClient: PackageClient,
    private val storageService: StorageService,
    private val storageManager: StorageManager,
    private val repositoryClient: RepositoryClient,
    private val ociProperties: OciProperties
) : OciOperationService {

    /**
     * 检查package 对应的version是否存在
     * 不存在则抛出异常OciFileNotFoundException
     */
    private fun checkVersionExist(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ) {
        packageClient.findVersionByName(
            projectId = projectId,
            repoName = repoName,
            packageKey = packageKey,
            version = version
        ).data ?: throw OciFileNotFoundException(
            "Could not get $packageKey/$version manifest file in repo: [$projectId/$repoName]",
            MANIFEST_UNKNOWN_CODE,
            MANIFEST_UNKNOWN_DESCRIPTION
        )
    }

    /**
     * 保存节点元数据
     */
    override fun saveMetaData(
        projectId: String,
        repoName: String,
        fullPath: String,
        metadata: MutableMap<String, Any>
    ) {
        val metadataSaveRequest = ObjectBuildUtils.buildMetadataSaveRequest(
            projectId = projectId,
            repoName = repoName,
            fullPath = fullPath,
            metadata = metadata
        )
        metadataClient.saveMetadata(metadataSaveRequest)
    }

    /**
     * 当mediaType为CHART_LAYER_MEDIA_TYPE，需要解析chart.yaml文件
     */
    override fun loadArtifactInput(
        chartDigest: String?,
        projectId: String,
        repoName: String,
        packageName: String,
        version: String,
        storageCredentials: StorageCredentials?
    ): Map<String, Any>? {
        if (chartDigest.isNullOrBlank()) return null
        val blobDigest = OciDigest(chartDigest)
        val fullPath = OciLocationUtils.buildDigestBlobsPath(packageName, blobDigest)
        nodeClient.getNodeDetail(projectId, repoName, fullPath).data?.let { node ->
            logger.info(
                "Will read chart.yaml data from $fullPath with package $packageName " +
                    "and version $version under repo $projectId/$repoName"
            )
            storageManager.loadArtifactInputStream(node, storageCredentials)?.let {
                return try {
                    OciUtils.convertToMap(OciUtils.parseChartInputStream(it))
                } catch (e: Exception) {
                    logger.warn("Convert chart.yaml error: ${e.message}")
                    null
                }
            }
        }
        return null
    }

    /**
     * 需要将blob中相关metadata写进package version中
     */
    override fun updatePackageInfo(
        ociArtifactInfo: OciArtifactInfo,
        packageKey: String,
        appVersion: String?,
        description: String?
    ) {
        with(ociArtifactInfo) {
            val packageUpdateRequest = ObjectBuildUtils.buildPackageUpdateRequest(
                artifactInfo = this,
                name = packageName,
                appVersion = appVersion,
                description = description,
                packageKey = packageKey
            )
            packageClient.updatePackage(packageUpdateRequest)
        }
    }

    /**
     * 删除package
     */
    fun remove(userId: String, artifactInfo: OciArtifactInfo) {
        with(artifactInfo) {
            // 可能存在支持多种type
            val repoDetail = getRepositoryInfo(artifactInfo)
            val packageKey = PackageKeys.ofName(repoDetail.type.name.toLowerCase(), packageName)
            if (version.isNotBlank()) {
                packageClient.findVersionByName(
                    projectId,
                    repoName,
                    packageKey,
                    version
                ).data?.let {
                    removeVersion(
                        artifactInfo = this,
                        version = it.name,
                        userId = userId,
                        packageKey = packageKey
                    )
                } ?: throw VersionNotFoundException(version)
            } else {
                packageClient.listAllVersion(
                    projectId,
                    repoName,
                    packageKey
                ).data.orEmpty().forEach {
                    removeVersion(
                        artifactInfo = this,
                        version = it.name,
                        userId = userId,
                        packageKey = packageKey
                    )
                }
            }
            updatePackageExtension(artifactInfo, packageKey)
        }
    }

    /**
     * 节点删除后，将package extension信息更新
     */
    private fun updatePackageExtension(artifactInfo: OciArtifactInfo, packageKey: String) {
        with(artifactInfo) {
            val version = packageClient.findPackageByKey(projectId, repoName, packageKey).data?.latest
            try {
                val chartDigest = findHelmChartYamlInfo(this, version)
                val chartYaml = loadArtifactInput(
                    chartDigest = chartDigest,
                    projectId = projectId,
                    repoName = repoName,
                    packageName = packageName,
                    version = version!!,
                    storageCredentials = getRepositoryInfo(this).storageCredentials
                )
                // 针对helm chart包，将部分信息放入到package中
                val (appVersion, description) = getMetaDataFromChart(chartYaml)
                updatePackageInfo(
                    ociArtifactInfo = artifactInfo,
                    appVersion = appVersion,
                    description = description,
                    packageKey = packageKey
                )
            } catch (e: Exception) {
                logger.warn("can not convert meta data")
            }
        }
    }

    /**
     * 删除[version] 对应的node节点也会一起删除
     */
    private fun removeVersion(
        artifactInfo: OciArtifactInfo,
        version: String,
        userId: String,
        packageKey: String
    ) {
        with(artifactInfo) {
            val nodeDetail = getBlobNodeDetail(
                projectId = projectId,
                repoName = repoName,
                name = artifactInfo.packageName,
                version = version
            ) ?: return
            // 删除manifest
            deleteNode(
                projectId = projectId,
                repoName = repoName,
                packageName = packageName,
                path = nodeDetail.fullPath,
                userId = userId
            )
            packageClient.deleteVersion(projectId, repoName, packageKey, version)
        }
    }

    /**
     * 针对helm特殊处理，查找chart对应的digest，用于读取对应的chart.yaml信息
     */
    private fun findHelmChartYamlInfo(artifactInfo: OciArtifactInfo, version: String? = null): String? {
        with(artifactInfo) {
            if (version.isNullOrBlank()) return null
            val nodeDetail = getBlobNodeDetail(
                projectId = projectId,
                repoName = repoName,
                name = artifactInfo.packageName,
                version = version
            ) ?: return null
            val inputStream = storageService.load(
                digest = nodeDetail.sha256!!,
                range = Range.full(nodeDetail.size),
                storageCredentials = getRepositoryInfo(artifactInfo).storageCredentials
            ) ?: return null
            try {
                val manifest = OciUtils.streamToManifestV2(inputStream)
                return (OciUtils.manifestIterator(manifest, CHART_LAYER_MEDIA_TYPE) ?: return null).digest
            } catch (e: OciBadRequestException) {
                logger.warn("Manifest convert error: ${e.message}")
            }
            return null
        }
    }

    private fun deleteNode(
        projectId: String,
        repoName: String,
        packageName: String,
        userId: String,
        digestStr: String? = null,
        path: String? = null
    ) {
        val fullPath = path ?: digestStr?.let {
            val nodeDetail = getBlobNodeDetail(
                projectId = projectId,
                repoName = repoName,
                name = packageName,
                digestStr = digestStr
            )
            nodeDetail?.fullPath
        }
        fullPath?.let {
            logger.info("Will delete node [$fullPath] with package $packageName in repo [$projectId/$repoName]")
            val request = NodeDeleteRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                operator = userId
            )
            nodeClient.deleteNode(request)
        }
    }

    /**
     * 查询仓库相关信息
     */
    private fun getRepositoryInfo(artifactInfo: OciArtifactInfo): RepositoryDetail {
        with(artifactInfo) {
            val result = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                ArtifactContextHolder.queryRepoDetailFormExtraRepoType(projectId, repoName)
            }
            return result
        }
    }

    override fun detailVersion(
        userId: String,
        artifactInfo: OciArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo {
        with(artifactInfo) {
            logger.info("Try to get detail of the [$packageKey/$version] in repo ${artifactInfo.getRepoIdentify()}")
            val repoDetail = getRepositoryInfo(artifactInfo)
            val name = PackageKeys.resolveName(repoDetail.type.name.toLowerCase(), packageKey)
            checkVersionExist(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                version = version
            )
            val nodeDetail = getBlobNodeDetail(
                projectId = projectId,
                repoName = repoName,
                name = name,
                version = version
            ) ?: throw OciFileNotFoundException("packageKey [$packageKey] don't found.")
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data!!
            val basicInfo = ObjectBuildUtils.buildBasicInfo(nodeDetail, packageVersion)
            return PackageVersionInfo(basicInfo, emptyMap())
        }
    }

    /**
     * 获取node节点
     * 查不到抛出OciFileNotFoundException异常
     */
    private fun getBlobNodeDetail(
        projectId: String,
        repoName: String,
        name: String,
        digestStr: String? = null,
        version: String = StringPool.EMPTY
    ): NodeDetail? {
        val ociArtifactInfo = if (digestStr == null) {
            // 返回manifest文件的节点信息
            OciManifestArtifactInfo(
                projectId = projectId,
                repoName = repoName,
                packageName = name,
                reference = version,
                isValidDigest = false,
                version = StringPool.EMPTY
            )
        } else {
            // 返回blob文件的节点信息
            OciBlobArtifactInfo(
                projectId = projectId,
                repoName = repoName,
                packageName = name,
                digest = digestStr,
                version = StringPool.EMPTY
            )
        }
        val fullPath = ociArtifactInfo.getArtifactFullPath()
        val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
            val oldDockerFullPath = getDockerNode(ociArtifactInfo) ?: return@run null
            nodeClient.getNodeDetail(projectId, repoName, oldDockerFullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                null
            }
        }
        return nodeDetail
    }

    override fun deletePackage(userId: String, artifactInfo: OciArtifactInfo) {
        logger.info("Try to delete the package [${artifactInfo.packageName}] in repo ${artifactInfo.getRepoIdentify()}")
        remove(userId, artifactInfo)
    }

    override fun deleteVersion(userId: String, artifactInfo: OciArtifactInfo) {
        logger.info(
            "Try to delete the package [${artifactInfo.packageName}/${artifactInfo.version}] " +
                "in repo ${artifactInfo.getRepoIdentify()}"
        )
        remove(userId, artifactInfo)
    }

    override fun getRegistryDomain(): String {
        return ociProperties.domain
    }

    /**
     * 构造节点创建请求
     */
    private fun buildNodeCreateRequest(
        ociArtifactInfo: OciArtifactInfo,
        artifactFile: ArtifactFile,
        proxyUrl: String? = null
    ): NodeCreateRequest {
        val metadata = proxyUrl?.let {
            mapOf(Pair(PROXY_URL, proxyUrl))
        }
        return ObjectBuildUtils.buildNodeCreateRequest(
            projectId = ociArtifactInfo.projectId,
            repoName = ociArtifactInfo.repoName,
            artifactFile = artifactFile,
            fullPath = ociArtifactInfo.getArtifactFullPath(),
            metadata = metadata
        )
    }

    /**
     * 保存文件内容(当使用追加上传时，文件已存储，只需存储节点信息)
     * 特殊：对于manifest文件，node存tag
     */
    override fun storeArtifact(
        ociArtifactInfo: OciArtifactInfo,
        artifactFile: ArtifactFile,
        storageCredentials: StorageCredentials?,
        fileInfo: FileInfo?,
        proxyUrl: String?
    ): NodeDetail? {
        val request = buildNodeCreateRequest(ociArtifactInfo, artifactFile, proxyUrl)
        return if (fileInfo != null) {
            val newNodeRequest = request.copy(
                size = fileInfo.size,
                md5 = fileInfo.md5,
                sha256 = fileInfo.sha256
            )
            createNode(newNodeRequest, storageCredentials)
            null
        } else {
            storageManager.storeArtifactFile(request, artifactFile, storageCredentials)
        }
    }

    /**
     * 当使用追加上传时，文件已存储，只需存储节点信息
     */
    override fun createNode(request: NodeCreateRequest, storageCredentials: StorageCredentials?): NodeDetail {
        try {
            return nodeClient.createNode(request).data!!
        } catch (exception: Exception) {
            // 当文件有创建，则删除文件
            try {
                storageService.delete(request.sha256!!, storageCredentials)
            } catch (exception: Exception) {
                logger.error("Failed to delete new created file[${request.sha256}]", exception)
            }
            // 异常往上抛
            throw exception
        }
    }

    /**
     * 更新整个blob相关信息,blob相关的mediatype，version等信息需要从manifest中获取
     */
    override fun updateOciInfo(
        ociArtifactInfo: OciManifestArtifactInfo,
        digest: OciDigest,
        artifactFile: ArtifactFile,
        fullPath: String,
        storageCredentials: StorageCredentials?
    ) {
        logger.info(
            "Will start to update oci info for ${ociArtifactInfo.getArtifactFullPath()} " +
                "in repo ${ociArtifactInfo.getRepoIdentify()}"
        )

        val version = OciUtils.checkVersion(artifactFile.getInputStream())
        val (mediaType, manifest) = if (version.schemaVersion == 1) {
            Pair(DOCKER_IMAGE_MANIFEST_MEDIA_TYPE_V1, null)
        } else {
            val manifest = OciUtils.streamToManifestV2(artifactFile.getInputStream())
            // 更新manifest文件的metadata
            val mediaTypeV2 = if (manifest.mediaType.isNullOrEmpty()) {
                HeaderUtils.getHeader(HttpHeaders.CONTENT_TYPE) ?: OCI_IMAGE_MANIFEST_MEDIA_TYPE
            } else {
                manifest.mediaType
            }
            Pair(mediaTypeV2, manifest)
        }

        updateNodeMetaData(
            projectId = ociArtifactInfo.projectId,
            repoName = ociArtifactInfo.repoName,
            version = ociArtifactInfo.reference,
            fullPath = fullPath,
            mediaType = mediaType!!
        )
        // 同步blob相关metadata
        if (ociArtifactInfo.packageName.isNotEmpty()) {
            if (version.schemaVersion == 1) {
                syncBlobInfoV1(
                    ociArtifactInfo = ociArtifactInfo,
                    manifestDigest = digest,
                    manifestPath = fullPath
                )
            } else {
                syncBlobInfo(
                    ociArtifactInfo = ociArtifactInfo,
                    manifest = manifest!!,
                    manifestDigest = digest,
                    storageCredentials = storageCredentials,
                    manifestPath = fullPath
                )
            }
        }
    }

    /**
     * 将部分信息存入节点metadata中
     */
    private fun updateNodeMetaData(
        projectId: String,
        repoName: String,
        version: String? = null,
        fullPath: String,
        mediaType: String,
        chartYaml: Map<String, Any>? = null
    ) {
        // 将基础信息存储到metadata中
        val metadata = ObjectBuildUtils.buildMetadata(
            mediaType = mediaType,
            version = version,
            yamlData = chartYaml
        )
        saveMetaData(
            projectId = projectId,
            repoName = repoName,
            fullPath = fullPath,
            metadata = metadata
        )
    }

    /**
     * 同步fsLayers层的数据
     */
    private fun syncBlobInfoV1(
        ociArtifactInfo: OciManifestArtifactInfo,
        manifestDigest: OciDigest,
        manifestPath: String
    ) {
        logger.info(
            "Will start to sync fsLayers' blob info from manifest ${ociArtifactInfo.getArtifactFullPath()} " +
                "to blobs in repo ${ociArtifactInfo.getRepoIdentify()}."
        )
        // 根据flag生成package信息以及packageversion信息
        doPackageOperations(
            manifestPath = manifestPath,
            ociArtifactInfo = ociArtifactInfo,
            manifestDigest = manifestDigest,
            size = 0
        )
    }

    /**
     * 同步blob层的数据和config里面的数据
     */
    private fun syncBlobInfo(
        ociArtifactInfo: OciManifestArtifactInfo,
        manifest: ManifestSchema2,
        manifestDigest: OciDigest,
        storageCredentials: StorageCredentials?,
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
                    loadArtifactInput(
                        chartDigest = it.digest,
                        projectId = ociArtifactInfo.projectId,
                        repoName = ociArtifactInfo.repoName,
                        packageName = ociArtifactInfo.packageName,
                        version = ociArtifactInfo.reference,
                        storageCredentials = storageCredentials
                    )
                }
                else -> null
            }
            doSyncBlob(it, ociArtifactInfo, chartYaml)
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
        yamlMap: Map<String, Any>? = null
    ) {
        with(ociArtifactInfo) {
            nodeClient.getNodeDetail(projectId, repoName, fullPath).data?.let {
                logger.info(
                    "The current blob [${descriptor.digest}] is stored in $fullPath with package $packageName " +
                        "and version $reference under repo ${getRepoIdentify()}"
                )
                updateNodeMetaData(
                    projectId = projectId,
                    repoName = repoName,
                    fullPath = it.fullPath,
                    mediaType = descriptor.mediaType,
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
            // 针对支持多仓库类型，如docker和oci
            val repoType = getRepositoryInfo(ociArtifactInfo).type.name
            val packageKey = PackageKeys.ofName(repoType.toLowerCase(), packageName)
            val packageVersion = packageClient.findVersionByName(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
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
                    metadata = metadata,
                    repoType = repoType
                )
                packageClient.createVersion(request)
            } else {
                val request = ObjectBuildUtils.buildPackageVersionUpdateRequest(
                    ociArtifactInfo = this,
                    version = ociArtifactInfo.reference,
                    size = size,
                    fullPath = manifestPath,
                    metadata = metadata,
                    packageKey = packageKey
                )
                packageClient.updateVersion(request)
            }

            // 针对helm chart包，将部分信息放入到package中
            val (appVersion, description) = getMetaDataFromChart(chartYaml)
            updatePackageInfo(
                ociArtifactInfo = ociArtifactInfo,
                appVersion = appVersion,
                description = description,
                packageKey = packageKey
            )
        }
    }

    /**
     * 针对helm chart包，将部分信息放入到package中
     */
    private fun getMetaDataFromChart(chartYaml: Map<String, Any>? = null): Pair<String?, String?> {
        var appVersion: String? = null
        var description: String? = null
        chartYaml?.let {
            appVersion = it[APP_VERSION] as String?
            description = it[DESCRIPTION] as String?
        }
        return Pair(appVersion, description)
    }

    /**
     * 获取对应存储节点路径
     * 特殊：manifest文件按tag存储， 但是查询时存在tag/digest
     */
    override fun getNodeFullPath(artifactInfo: OciArtifactInfo): String? {
        if (artifactInfo is OciManifestArtifactInfo) {
            // 根据类型解析实际存储路径，manifest获取路径有tag/digest
            if (artifactInfo.isValidDigest) {
                return getNodeByDigest(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    digestStr = artifactInfo.reference
                )
            }
        }
        return artifactInfo.getArtifactFullPath()
    }

    /**
     * 根据sha256值获取对应的node fullpath
     */
    override fun getNodeByDigest(
        projectId: String,
        repoName: String,
        digestStr: String
    ): String? {
        val ociDigest = OciDigest(digestStr)
        val queryModel = NodeQueryBuilder()
            .select(NODE_FULL_PATH)
            .projectId(projectId)
            .repoName(repoName)
            .sha256(ociDigest.getDigestHex())
            .sortByAsc(NODE_FULL_PATH)
        val result = nodeClient.search(queryModel.build()).data ?: run {
            logger.warn(
                "Could not find $digestStr " +
                    "in repo $projectId|$repoName"
            )
            return null
        }
        if (result.records.isEmpty()) return null
        return result.records[0][NODE_FULL_PATH] as String
    }

    /**
     * 针对老的docker仓库的数据做兼容性处理
     * 老版数据node存储格式不一样：
     * 1 docker-local/nginx/latest 下存所有manifest和blobs
     * 2 docker-local/nginx/_uploads/ 临时存储上传的blobs，待manifest文件上传成功后移到到对应版本下，如docker-local/nginx/latest
     */
    override fun getDockerNode(artifactInfo: OciArtifactInfo): String? {
        if (artifactInfo is OciManifestArtifactInfo) {
            // 根据类型解析实际存储路径，manifest获取路径有tag/digest
            if (artifactInfo.isValidDigest)
                return getNodeByDigest(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    digestStr = artifactInfo.reference
                )
            return "/${artifactInfo.packageName}/${artifactInfo.reference}/manifest.json"
        }
        if (artifactInfo is OciBlobArtifactInfo) {
            val digestStr = artifactInfo.digest ?: StringPool.EMPTY
            return getNodeByDigest(
                projectId = artifactInfo.projectId,
                repoName = artifactInfo.repoName,
                digestStr = digestStr
            )
        }
        return null
    }

    override fun getReturnDomain(request: HttpServletRequest): String {
        return OciResponseUtils.getResponseURI(
            request = request,
            enableHttp = ociProperties.http
        ).toString()
    }

    override fun getManifest(artifactInfo: OciManifestArtifactInfo): String {
        val context = ArtifactQueryContext()
        try {
            val inputStream = ArtifactContextHolder.getRepository().query(context) ?: throw OciFileNotFoundException(
                "Error occurred when querying the manifest file.. "
            )
            return (inputStream as ArtifactInputStream).readBytes().toString(Charset.defaultCharset())
        } catch (e: Exception) {
            logger.warn(e.message.toString())
            throw OciFileNotFoundException(e.message.toString())
        }
    }

    override fun getImageList(
        projectId: String,
        repoName: String,
        pageNumber: Int,
        pageSize: Int,
        name: String?
    ): OciImageResult {
        return getOciArtifactList(
            projectId = projectId,
            repoName = repoName,
            pageNumber = pageNumber,
            pageSize = pageSize,
            name = name
        )
    }

    fun getOciArtifactList(
        projectId: String,
        repoName: String,
        pageNumber: Int,
        pageSize: Int,
        name: String?
    ): OciImageResult {
        val queryModel = PackageQueryBuilder().select(
            OCI_PACKAGE_NAME,
            LAST_MODIFIED_BY,
            LAST_MODIFIED_DATE,
            DESCRIPTION,
            DOWNLOADS
        ).projectId(projectId).repoName(repoName).sortByAsc(OCI_NODE_FULL_PATH)
            .page(pageNumber, pageSize)
        name?.let {
            queryModel.name("*$name*", OperationType.MATCH)
        }
        val result = packageClient.searchPackage(queryModel.build()).data ?: run {
            logger.warn("find repo list failed: [$projectId, $repoName] ")
            return OciImageResult(0, emptyList())
        }
        val data = mutableListOf<OciImage>()
        result.records.forEach {
            val imageName = it[OCI_PACKAGE_NAME] as String
            val lastModifiedBy = it[LAST_MODIFIED_BY] as String
            val lastModifiedDate = it[LAST_MODIFIED_DATE] as Long
            val downLoadCount = it[DOWNLOADS] as Int
            val description = it[DESCRIPTION] as String? ?: StringPool.EMPTY
            val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModifiedDate), ZoneId.systemDefault())
            data.add(
                OciImage(
                    name = imageName,
                    lastModifiedBy = lastModifiedBy,
                    lastModifiedDate = localDateTime.toString(),
                    downloadCount = downLoadCount.toLong(),
                    logoUrl = StringPool.EMPTY,
                    description = description
                )
            )
        }
        return OciImageResult(result.totalRecords, data)
    }

    override fun getRepoTag(
        projectId: String,
        repoName: String,
        pageNumber: Int,
        packageName: String,
        pageSize: Int,
        tag: String?
    ): OciTagResult {
        val artifactInfo = OciArtifactInfo(projectId, repoName, StringPool.EMPTY, StringPool.EMPTY)
        val repoDetail = getRepositoryInfo(artifactInfo)
        val packageKey = PackageKeys.ofName(repoDetail.type.name.toLowerCase(), packageName)
        val result = packageClient.listVersionPage(
            projectId,
            repoName,
            packageKey,
            VersionListOption(pageNumber, pageSize, tag, null)
        ).data ?: return OciTagResult(0, emptyList())
        val data = mutableListOf<OciTag>()
        result.records.forEach {
            val name = it.name
            val stageTag = StringUtils.join(it.stageTag.toTypedArray())
            val size = it.size
            val lastModifiedBy = it.lastModifiedBy
            val lastModifiedDate = it.lastModifiedDate.toString()
            val downLoadCount = it.downloads
            val registryUrl = "${ociProperties.domain}/$projectId/$repoName/$packageName:$name"
            data.add(
                OciTag(name, stageTag, size, lastModifiedBy, lastModifiedDate, downLoadCount, registryUrl)
            )
        }
        return OciTagResult(result.totalRecords, data)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OciOperationServiceImpl::class.java)
    }
}
