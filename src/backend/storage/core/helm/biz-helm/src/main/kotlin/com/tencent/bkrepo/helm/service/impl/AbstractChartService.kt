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

package com.tencent.bkrepo.helm.service.impl

import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.api.util.toYamlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyChannelSetting
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResourceWriter
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.lock.service.LockOperation
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.exception.RemoteErrorCodeException
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.CHART_PACKAGE_FILE_EXTENSION
import com.tencent.bkrepo.helm.constants.FILE_TYPE
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.META_DETAIL
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.NODE_CREATE_DATE
import com.tencent.bkrepo.helm.constants.NODE_FULL_PATH
import com.tencent.bkrepo.helm.constants.NODE_METADATA
import com.tencent.bkrepo.helm.constants.NODE_NAME
import com.tencent.bkrepo.helm.constants.NODE_SHA256
import com.tencent.bkrepo.helm.constants.OVERWRITE
import com.tencent.bkrepo.helm.constants.PROJECT_ID
import com.tencent.bkrepo.helm.constants.REDIS_LOCK_KEY_PREFIX
import com.tencent.bkrepo.helm.constants.REPO_NAME
import com.tencent.bkrepo.helm.constants.REPO_TYPE
import com.tencent.bkrepo.helm.constants.SIZE
import com.tencent.bkrepo.helm.constants.TGZ_SUFFIX
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.exception.HelmBadRequestException
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.exception.HelmRepoNotFoundException
import com.tencent.bkrepo.helm.pojo.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.pool.HelmThreadPoolExecutor
import com.tencent.bkrepo.helm.utils.ChartParserUtil
import com.tencent.bkrepo.helm.utils.DecompressUtil.getArchivesContent
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import com.tencent.bkrepo.helm.utils.RemoteDownloadUtil
import com.tencent.bkrepo.helm.utils.TimeFormatUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PopulatedPackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.SortedSet
import java.util.concurrent.ThreadPoolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

// LateinitUsage: 抽象类中使用构造器注入会造成不便
@Suppress("LateinitUsage")
open class AbstractChartService : ArtifactService() {
    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var metadataClient: MetadataClient

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    @Autowired
    lateinit var packageClient: PackageClient

    @Autowired
    lateinit var artifactResourceWriter: ArtifactResourceWriter

    @Autowired
    lateinit var storageManager: StorageManager

    @Autowired
    lateinit var lockOperation: LockOperation

    @Autowired
    lateinit var proxyChannelClient: ProxyChannelClient

    val threadPoolExecutor: ThreadPoolExecutor = HelmThreadPoolExecutor.instance

    fun queryOriginalIndexYaml(): HelmIndexYamlMetadata {
        val context = ArtifactQueryContext()
        context.putAttribute(FULL_PATH, HelmUtils.getIndexCacheYamlFullPath())
        try {
            val inputStream = ArtifactContextHolder.getRepository().query(context) ?: throw HelmFileNotFoundException(
                "Error occurred when querying the index.yaml file.. "
            )
            return (inputStream as ArtifactInputStream).use { it.readYamlString() }
        } catch (e: Exception) {
            logger.warn("Error occurred while querying index.yaml, error: ${e.message}")
            throw HelmFileNotFoundException(e.message.toString())
        }
    }

    /**
     * query original index.yaml file
     */
    fun getOriginalIndexYaml(projectId: String, repoName: String): HelmIndexYamlMetadata {
        val nodeDetail = getOriginalIndexNode(projectId, repoName)
        val repository = repositoryClient.getRepoDetail(projectId, repoName, RepositoryType.HELM.name).data
            ?: throw RepoNotFoundException("Repository[$repoName] does not exist")
        val inputStream = storageManager.loadArtifactInputStream(nodeDetail, repository.storageCredentials)
            ?: throw HelmFileNotFoundException("Artifact index.yaml does not exist")
        return inputStream.use { it.readYamlString() }
    }

    fun getOriginalIndexNode(projectId: String, repoName: String): NodeDetail? {
        val fullPath = HelmUtils.getIndexCacheYamlFullPath()
        return nodeClient.getNodeDetail(projectId, repoName, fullPath).data
    }

    /**
     * upload index.yaml file
     */
    fun uploadIndexYamlMetadata(indexYamlMetadata: HelmIndexYamlMetadata) {
        val artifactFile = ArtifactFileFactory.build(indexYamlMetadata.toYamlString().byteInputStream())
        val context = ArtifactUploadContext(artifactFile)
        context.putAttribute(FULL_PATH, HelmUtils.getIndexCacheYamlFullPath())
        ArtifactContextHolder.getRepository().upload(context)
    }

    /**
     * upload index.yaml file
     */
    fun uploadIndexYamlMetadata(artifactFile: ArtifactFile, nodeCreateRequest: NodeCreateRequest) {
        val repository = repositoryClient.getRepoDetail(
            nodeCreateRequest.projectId,
            nodeCreateRequest.repoName,
            RepositoryType.HELM.name
        ).data
            ?: throw RepoNotFoundException("Repository[${nodeCreateRequest.repoName}] does not exist")
        storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, repository.storageCredentials)
    }

    /**
     * 查询仓库相关信息
     */
    fun getRepositoryInfo(artifactInfo: ArtifactInfo): RepositoryDetail {
        with(artifactInfo) {
            val result = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                logger.warn("check repository [$repoName] in projectId [$projectId] failed!")
                throw HelmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
            }
            return result
        }
    }

    /**
     * 根据路径取读取chart的Chart.yaml文件
     */
    fun queryHelmChartMetadata(context: ArtifactQueryContext, path: String): HelmChartMetadata {
        context.putAttribute(FULL_PATH, path)
        val artifactInputStream =
            ArtifactContextHolder.getRepository().query(context) as ArtifactInputStream
        context.putAttribute(SIZE, artifactInputStream.range.length)
        val content = artifactInputStream.use {
            it.getArchivesContent(CHART_PACKAGE_FILE_EXTENSION)
        }
        return content.byteInputStream().readYamlString()
    }

    /**
     * 查询仓库是否存在，以及仓库类型
     */
    fun checkRepositoryExistAndCategory(artifactInfo: ArtifactInfo) {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                logger.warn("check repository [$repoName] in projectId [$projectId] failed!")
                throw HelmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
            }
            when (repo.category) {
                RepositoryCategory.REMOTE -> throw HelmBadRequestException(
                    "Unable to upload chart into a remote repository [$projectId/$repoName]"
                )
                else -> return
            }
        }
    }

    /**
     * 当helm 本地文件上传后/或从远程代理下载后，创建或更新包/包版本信息
     */
    fun initPackageInfo(context: ArtifactContext) {
        with(context) {
            if (CHART != getStringAttribute(FILE_TYPE)) return
            logger.info("start to update package meta info..")
            val size = getLongAttribute(SIZE)
            val helmChartMetadataMap = getAttribute<Map<String, Any>?>(META_DETAIL)
            helmChartMetadataMap?.let {
                val helmChartMetadata = HelmMetadataUtils.convertToObject(helmChartMetadataMap)
                val overWrite = getBooleanAttribute(OVERWRITE) ?: false
                createVersion(
                    userId = userId,
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    chartInfo = helmChartMetadata,
                    size = size!!,
                    isOverwrite = overWrite
                )
            }
        }
    }

    /**
     * 查询节点
     */
    fun queryNodeList(
        artifactInfo: HelmArtifactInfo,
        exist: Boolean = true,
        lastModifyTime: LocalDateTime? = null
    ): List<Map<String, Any?>> {
        with(artifactInfo) {
            val queryModelBuilder = NodeQueryBuilder()
                .select(PROJECT_ID, REPO_NAME, NODE_NAME, NODE_FULL_PATH, NODE_METADATA, NODE_SHA256, NODE_CREATE_DATE)
                .sortByAsc(NODE_FULL_PATH)
                .page(PAGE_NUMBER, PAGE_SIZE)
                .projectId(projectId)
                .repoName(repoName)
                .fullPath(TGZ_SUFFIX, OperationType.SUFFIX)
            if (exist) {
                lastModifyTime?.let { queryModelBuilder.rule(true, NODE_CREATE_DATE, it, OperationType.AFTER) }
            }
            val result = nodeClient.search(queryModelBuilder.build()).data ?: run {
                logger.warn("don't find node list in repository: [$projectId/$repoName].")
                return emptyList()
            }
            return result.records
        }
    }

    /**
     * check node exists
     */
    fun exist(projectId: String, repoName: String, fullPath: String): Boolean {
        return nodeClient.checkExist(projectId, repoName, fullPath).data ?: false
    }

    /**
     * check package [key] version [version] exists
     */
    fun packageVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
        return packageClient.findVersionByName(projectId, repoName, key, version).data?.let { true } ?: false
    }

    /**
     * check package [key] exists
     */
    fun packageExist(projectId: String, repoName: String, key: String): Boolean {
        return packageClient.findPackageByKey(projectId, repoName, key).data?.let { true } ?: false
    }

    /**
     * 包版本数据填充
     * [nodeInfo] 某个节点的node节点信息
     */
    fun populatePackage(
        packageVersionList: List<PopulatedPackageVersion>,
        nodeInfo: NodeInfo,
        name: String,
        description: String
    ) {
        with(nodeInfo) {
            val packagePopulateRequest = PackagePopulateRequest(
                createdBy = nodeInfo.createdBy,
                createdDate = LocalDateTime.parse(createdDate),
                lastModifiedBy = nodeInfo.lastModifiedBy,
                lastModifiedDate = LocalDateTime.parse(lastModifiedDate),
                projectId = nodeInfo.projectId,
                repoName = nodeInfo.repoName,
                name = name,
                key = PackageKeys.ofHelm(name),
                type = PackageType.HELM,
                description = description,
                versionList = packageVersionList
            )
            packageClient.populatePackage(packagePopulateRequest)
        }
    }

    /**
     * 创建包版本
     */
    fun createVersion(
        userId: String,
        projectId: String,
        repoName: String,
        chartInfo: HelmChartMetadata,
        size: Long = 0,
        isOverwrite: Boolean = false
    ) {
        val contentPath = HelmUtils.getChartFileFullPath(chartInfo.name, chartInfo.version)
        val packageVersionCreateRequest = ObjectBuilderUtil.buildPackageVersionCreateRequest(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            chartInfo = chartInfo,
            size = size,
            isOverwrite = isOverwrite
        )
        val packageUpdateRequest = ObjectBuilderUtil.buildPackageUpdateRequest(
            projectId = projectId,
            repoName = repoName,
            chartInfo = chartInfo
        )
        try {
            packageClient.createVersion(packageVersionCreateRequest).apply {
                logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
            }
            packageClient.updatePackage(packageUpdateRequest)
        } catch (exception: RemoteErrorCodeException) {
            // 暂时转换为包存在异常
            logger.warn("package version for $contentPath already existed, message: ${exception.message}")
        }
    }

    private fun buildRedisKey(projectId: String, repoName: String): String {
        return "$REDIS_LOCK_KEY_PREFIX$projectId/$repoName"
    }

    /**
     * 针对自旋达到次数后，还没有获取到锁的情况默认也会执行所传入的方法,确保业务流程不中断
     */
    fun <T> lockAction(projectId: String, repoName: String, action: () -> T): T {
        val lockKey = buildRedisKey(projectId, repoName)
        val lock = lockOperation.getLock(lockKey)
        return if (lockOperation.getSpinLock(lockKey, lock)) {
            LockOperation.logger.info("Lock for key $lockKey has been acquired.")
            try {
                action()
            } finally {
                lockOperation.close(lockKey, lock)
            }
        } else {
            action()
        }
    }

    /**
     * 通过chart包名以及版本号查出对应remote仓库下载地址
     */
    fun findRemoteArtifactFullPath(name: String): String {
        logger.info("get remote url for downloading...")
        val helmIndexYamlMetadata = queryOriginalIndexYaml()
        val map = ChartParserUtil.parseNameAndVersion(name)
        val chartName = map[NAME]
        val chartVersion = map[VERSION]
        val chartList =
            helmIndexYamlMetadata.entries[chartName]
                ?: throw HelmFileNotFoundException("File [$name] can not be found.")
        val helmChartMetadataList = chartList.filter {
            chartVersion == it.version
        }.toList()
        return if (helmChartMetadataList.isNotEmpty()) {
            require(helmChartMetadataList.size == 1) {
                "find more than one version [$chartVersion] in package [$chartName]."
            }
            val urls = helmChartMetadataList.first().urls
            if (urls.isNotEmpty()) {
                urls.first()
            } else {
                throw HelmFileNotFoundException("File [$name] can not be found.")
            }
        } else {
            throw HelmFileNotFoundException("File [$name] can not be found.")
        }
    }

    /**
     * 针对代理仓库：下载index.yaml文件到本地存储
     */
    fun initIndexYaml(
        projectId: String,
        repoName: String,
        userId: String = SecurityUtils.getUserId()
    ): HelmIndexYamlMetadata? {
        logger.info("Will start to get index.yaml for repo [$projectId/$repoName]...")
        val repoDetail = checkRepo(projectId, repoName) ?: return null
        val originalIndexYamlMetadata = getIndex(repoDetail)
        originalIndexYamlMetadata?.let {
            storeIndex(
                indexYamlMetadata = originalIndexYamlMetadata,
                projectId = projectId,
                repoName = repoName,
                userId = userId
            )
        }
        return originalIndexYamlMetadata
    }

    /**
     * 根据仓库类型获取对应index文件
     */
    fun getIndex(repoDetail: RepositoryDetail): HelmIndexYamlMetadata? {
        return when (repoDetail.configuration) {
            is CompositeConfiguration -> {
                val config = repoDetail.configuration as CompositeConfiguration
                forEachProxyRepo(config.proxy.channelList, repoDetail)
            }
            is RemoteConfiguration -> {
                val config = repoDetail.configuration as RemoteConfiguration
                try {
                    val inputStream = RemoteDownloadUtil.doHttpRequest(config, HelmUtils.getIndexYamlFullPath())
                    (inputStream as ArtifactInputStream).use {
                        it.readYamlString() as HelmIndexYamlMetadata
                    }
                } catch (e: Exception) {
                    logger.warn(
                        "Error occurred while caching the index-cache.yaml from remote repository, error: ${e.message}"
                    )
                    throw e
                }
            }
            else -> {
                null
            }
        }
    }

    /**
     * 存储获取的index文件
     */
    fun storeIndex(
        indexYamlMetadata: HelmIndexYamlMetadata,
        projectId: String,
        repoName: String,
        userId: String = SecurityUtils.getUserId()
    ) {
        logger.info("Index file will be stored in repo $projectId|$repoName")
        val (artifactFile, nodeCreateRequest) = ObjectBuilderUtil.buildFileAndNodeCreateRequest(
            indexYamlMetadata = indexYamlMetadata,
            projectId = projectId,
            repoName = repoName,
            operator = userId
        )
        uploadIndexYamlMetadata(artifactFile, nodeCreateRequest)
    }

    /**
     * 检查该仓库是否remote仓库或者composite仓库
     */
    fun checkRepo(projectId: String, repoName: String): RepositoryDetail? {
        val repoDetail = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
            throw HelmRepoNotFoundException("Could not find helm repository named [$repoName] in project [$projectId].")
        }
        if (RepositoryCategory.LOCAL == repoDetail.category) {
            logger.warn(
                "The local repo [$projectId/$repoName] does not need to do some operations on index.yaml"
            )
            return null
        }
        return repoDetail
    }

    /**
     * 遍历代理仓库列表，获取对应index.yaml文件
     */
    private fun forEachProxyRepo(
        channelList: List<ProxyChannelSetting>,
        repositoryDetail: RepositoryDetail
    ): HelmIndexYamlMetadata {
        val oldIndex = HelmUtils.initIndexYamlMetadata()
        for (proxyChannel in channelList) {
            try {
                val config = getRemoteConfigFromProxyChannel(repositoryDetail, proxyChannel)
                val inputStream = RemoteDownloadUtil.doHttpRequest(config, HelmUtils.getIndexYamlFullPath())
                val newIndex = (inputStream as ArtifactInputStream).use {
                    it.readYamlString() as HelmIndexYamlMetadata
                }
                val (_, addedSet) = ChartParserUtil.compareIndexYamlMetadata(
                    oldEntries = oldIndex.entries,
                    newEntries = newIndex.entries
                )
                mergeMap(oldIndex.entries, addedSet)
            } catch (ignored: Exception) {
                logger.warn("Failed to execute action with channel ${proxyChannel.name}", ignored)
            }
        }
        return oldIndex
    }

    /**
     * 合并
     */
    private fun mergeMap(
        old: MutableMap<String, SortedSet<HelmChartMetadata>>,
        new: MutableMap<String, SortedSet<HelmChartMetadata>>
    ) {
        new.forEach { (key, values) ->
            old[key]?.let {
                old[key]?.addAll(values)
            } ?: run { old.put(key, values) }
        }
    }

    private fun getRemoteConfigFromProxyChannel(
        repositoryDetail: RepositoryDetail,
        setting: ProxyChannelSetting
    ): RemoteConfiguration {
        val proxyChannel = proxyChannelClient.getByUniqueId(
            projectId = repositoryDetail.projectId,
            repoName = repositoryDetail.name,
            repoType = repositoryDetail.type.name,
            name = setting.name
        ).data!!
        // 构造RemoteConfiguration
        return (CompositeRepository.convertConfig(proxyChannel) as RemoteConfiguration)
    }

    companion object {
        const val PAGE_NUMBER = 0
        const val PAGE_SIZE = 100000
        val logger: Logger = LoggerFactory.getLogger(AbstractChartService::class.java)

        fun convertDateTime(timeStr: String): String {
            val localDateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME)
            return TimeFormatUtil.convertToUtcTime(localDateTime)
        }
    }
}
