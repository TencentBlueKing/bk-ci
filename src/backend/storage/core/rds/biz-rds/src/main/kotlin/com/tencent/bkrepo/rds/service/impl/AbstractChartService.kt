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

package com.tencent.bkrepo.rds.service.impl

import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.api.util.toYamlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResourceWriter
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.lock.service.LockOperation
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.service.exception.RemoteErrorCodeException
import com.tencent.bkrepo.rds.constants.CHART
import com.tencent.bkrepo.rds.constants.FILE_TYPE
import com.tencent.bkrepo.rds.constants.FULL_PATH
import com.tencent.bkrepo.rds.constants.META_DETAIL
import com.tencent.bkrepo.rds.constants.NODE_CREATE_DATE
import com.tencent.bkrepo.rds.constants.NODE_FULL_PATH
import com.tencent.bkrepo.rds.constants.NODE_METADATA
import com.tencent.bkrepo.rds.constants.NODE_NAME
import com.tencent.bkrepo.rds.constants.NODE_SHA256
import com.tencent.bkrepo.rds.constants.OVERWRITE
import com.tencent.bkrepo.rds.constants.PROJECT_ID
import com.tencent.bkrepo.rds.constants.REDIS_LOCK_KEY_PREFIX
import com.tencent.bkrepo.rds.constants.REPO_NAME
import com.tencent.bkrepo.rds.constants.REPO_TYPE
import com.tencent.bkrepo.rds.constants.SIZE
import com.tencent.bkrepo.rds.exception.RdsBadRequestException
import com.tencent.bkrepo.rds.exception.RdsFileAlreadyExistsException
import com.tencent.bkrepo.rds.exception.RdsFileNotFoundException
import com.tencent.bkrepo.rds.exception.RdsRepoNotFoundException
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo
import com.tencent.bkrepo.rds.pojo.metadata.RdsChartMetadata
import com.tencent.bkrepo.rds.pojo.metadata.RdsIndexYamlMetadata
import com.tencent.bkrepo.rds.pool.RdsThreadPoolExecutor
import com.tencent.bkrepo.rds.utils.ObjectBuilderUtil
import com.tencent.bkrepo.rds.utils.RdsMetadataUtils
import com.tencent.bkrepo.rds.utils.RdsUtils
import com.tencent.bkrepo.rds.utils.TimeFormatUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadPoolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher

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
    lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var artifactResourceWriter: ArtifactResourceWriter

    @Autowired
    lateinit var storageManager: StorageManager

    @Autowired
    lateinit var lockOperation: LockOperation

    val threadPoolExecutor: ThreadPoolExecutor = RdsThreadPoolExecutor.instance

    fun queryOriginalIndexYaml(): RdsIndexYamlMetadata {
        val context = ArtifactQueryContext()
        context.putAttribute(FULL_PATH, RdsUtils.getIndexCacheYamlFullPath())
        try {
            val inputStream = ArtifactContextHolder.getRepository().query(context) ?: throw RdsFileNotFoundException(
                "Error occurred when querying the index.yaml file.. "
            )
            return (inputStream as ArtifactInputStream).use { it.readYamlString() }
        } catch (e: Exception) {
            logger.error("Error occurred while querying index.yaml, error: ${e.message}")
            throw RdsFileNotFoundException(e.message.toString())
        }
    }

    /**
     * query original index.yaml file
     */
    fun getOriginalIndexYaml(projectId: String, repoName: String): RdsIndexYamlMetadata {
        val fullPath = RdsUtils.getIndexCacheYamlFullPath()
        val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        val repository = repositoryClient.getRepoDetail(projectId, repoName, RepositoryType.RDS.name).data
            ?: throw RepoNotFoundException("Repository[$repoName] does not exist")
        val inputStream = storageManager.loadArtifactInputStream(nodeDetail, repository.storageCredentials)
            ?: throw RdsFileNotFoundException("Artifact[$fullPath] does not exist")
        return inputStream.use { it.readYamlString() }
    }

    /**
     * 下载index.yaml （local类型仓库index.yaml存储时使用的name时index-cache.yaml，remote需要转换）
     */
    fun downloadIndexYaml() {
        val context = ArtifactDownloadContext(null, ObjectBuilderUtil.buildIndexYamlRequest())
        context.putAttribute(FULL_PATH, RdsUtils.getIndexCacheYamlFullPath())
        try {
            ArtifactContextHolder.getRepository().download(context)
        } catch (e: Exception) {
            logger.error("Error occurred while downloading index.yaml, error: ${e.message}")
            throw RdsFileNotFoundException(e.message.toString())
        }
    }
    /**
     * upload index.yaml file
     */
    fun uploadIndexYamlMetadata(indexYamlMetadata: RdsIndexYamlMetadata) {
        val artifactFile = ArtifactFileFactory.build(indexYamlMetadata.toYamlString().byteInputStream())
        val context = ArtifactUploadContext(artifactFile)
        context.putAttribute(FULL_PATH, RdsUtils.getIndexCacheYamlFullPath())
        ArtifactContextHolder.getRepository().upload(context)
    }

    /**
     * upload index.yaml file
     */
    fun uploadIndexYamlMetadata(artifactFile: ArtifactFile, nodeCreateRequest: NodeCreateRequest) {
        val repository = repositoryClient.getRepoDetail(
            nodeCreateRequest.projectId,
            nodeCreateRequest.repoName,
            RepositoryType.RDS.name
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
                throw RdsRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
            }
            return result
        }
    }

    /**
     * 查询仓库是否存在，以及仓库类型
     */
    fun checkRepositoryExistAndCategory(artifactInfo: ArtifactInfo) {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                logger.warn("check repository [$repoName] in projectId [$projectId] failed!")
                throw RdsRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
            }
            when (repo.category) {
                RepositoryCategory.REMOTE -> throw RdsBadRequestException(
                    "Unable to upload chart into a remote repository [$projectId/$repoName]"
                )
                else -> return
            }
        }
    }

    /**
     * 当helm 本地文件上传后，创建或更新包/包版本信息
     */
    fun initPackageInfo(context: ArtifactContext) {
        with(context) {
            logger.info("start to update package meta info..")
            if (CHART != getStringAttribute(FILE_TYPE)) return
            val size = getLongAttribute(SIZE)
            val rdsChartMetadataMap = getAttribute<Map<String, Any>?>(META_DETAIL)
            rdsChartMetadataMap?.let {
                val rdsChartMetadata = RdsMetadataUtils.convertToObject(rdsChartMetadataMap)
                val overWrite = getBooleanAttribute(OVERWRITE) ?: false
                createVersion(userId, artifactInfo, rdsChartMetadata, size!!, overWrite)
            }
        }
    }

    /**
     * 查询节点
     */
    fun queryNodeList(
        artifactInfo: RdsArtifactInfo,
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
                .excludeFolder()
//                .fullPath(TGZ_SUFFIX, OperationType.SUFFIX)
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
     * 发布事件
     */
    fun publishEvent(any: Any) {
        eventPublisher.publishEvent(any)
    }

    /**
     * 创建包版本
     */
    fun createVersion(
        userId: String,
        artifactInfo: ArtifactInfo,
        chartInfo: RdsChartMetadata,
        size: Long,
        isOverwrite: Boolean = false
    ) {
        val contentPath = RdsUtils.getChartFileFullPath(chartInfo.code, chartInfo.version, chartInfo.extension)
        val packageVersionCreateRequest = ObjectBuilderUtil.buildPackageVersionCreateRequest(
            userId = userId,
            artifactInfo = artifactInfo,
            chartInfo = chartInfo,
            size = size,
            isOverwrite = isOverwrite
        )
        val packageUpdateRequest = ObjectBuilderUtil.buildPackageUpdateRequest(artifactInfo, chartInfo)
        try {
            packageClient.createVersion(packageVersionCreateRequest).apply {
                logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
            }
            packageClient.updatePackage(packageUpdateRequest)
        } catch (exception: RemoteErrorCodeException) {
            // 暂时转换为包存在异常
            logger.warn("$contentPath already exists, message: ${exception.message}")
            throw RdsFileAlreadyExistsException("$contentPath already exists")
        }
    }

    /**
     * 下载index.yaml文件到本地存储
     */
    fun initIndexYaml(projectId: String, repoName: String) {
        logger.info("repo [$projectId/$repoName] has been created, will download index.yaml...")
        val repoDetail = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
            logger.warn("check repository [$repoName] in projectId [$projectId] failed!")
            throw RdsRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
        }
        if (RepositoryCategory.REMOTE != repoDetail.category) {
            logger.warn("repo [$projectId/$repoName] does not need to download index.yaml")
            return
        }
        val fullPath = RdsUtils.getIndexCacheYamlFullPath()
        val context = ArtifactDownloadContext(repoDetail, ObjectBuilderUtil.buildIndexYamlRequest(projectId, repoName))
        nodeClient.deleteNode(
            NodeDeleteRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                operator = context.userId
            )
        )
        context.putAttribute(FULL_PATH, fullPath)
        ArtifactContextHolder.getRepository().download(context)
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
