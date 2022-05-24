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

package com.tencent.bkrepo.common.artifact.manager

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.cluster.ClusterProperties
import com.tencent.bkrepo.common.artifact.cluster.FeignClientFactory
import com.tencent.bkrepo.common.artifact.cluster.RoleType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.EmptyInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.http.HttpRangeUtils.resolveRange
import com.tencent.bkrepo.common.service.util.HttpContextHolder.getRequestOrNull
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.innercos.http.HttpMethod
import com.tencent.bkrepo.replication.api.BlobReplicaClient
import com.tencent.bkrepo.replication.pojo.blob.BlobPullRequest
import com.tencent.bkrepo.replication.pojo.cluster.RemoteClusterInfo
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory

/**
 * 存储管理类
 *
 * 虽然[StorageService]提供了构件存储服务，但保存一个文件节点需要两步操作:
 *   1. [StorageService]保存文件数据
 *   2. [NodeClient]微服务调用创建文件节点
 * 这样会存在几个问题:
 *   1. 每个地方都会进行同样的操作，增加代码重复率
 *   2. 不支持事务，如果文件保存成功，但节点创建失败，会导致产生垃圾文件并且无法清理
 *
 * 所以提供StorageManager，简化依赖源的操作并减少错误率
 * addition: 增加了向中心节点代理拉取文件的逻辑
 */
@Suppress("TooGenericExceptionCaught")
class StorageManager(
    private val storageService: StorageService,
    private val nodeClient: NodeClient,
    private val clusterProperties: ClusterProperties,
    private val storageCredentialsClient: StorageCredentialsClient
) {

    /**
     * 中心节点集群信息
     */
    private val centerClusterInfo = RemoteClusterInfo(
        url = clusterProperties.center.url.orEmpty(),
        username = clusterProperties.center.username.orEmpty(),
        password = clusterProperties.center.password.orEmpty(),
        certificate = clusterProperties.center.certificate.orEmpty()
    )

    /**
     * 存储同步client
     */
    private val blobReplicaClient: BlobReplicaClient by lazy {
        FeignClientFactory.create<BlobReplicaClient>(centerClusterInfo)
    }

    /**
     * 存储构件[artifactFile]到[storageCredentials]上，并根据[request]创建节点
     * 操作成功返回节点详情[NodeDetail]
     */
    fun storeArtifactFile(
        request: NodeCreateRequest,
        artifactFile: ArtifactFile,
        storageCredentials: StorageCredentials?
    ): NodeDetail {
        val affectedCount = storageService.store(request.sha256!!, artifactFile, storageCredentials)
        try {
            return nodeClient.createNode(request).data!!
        } catch (exception: Exception) {
            // 当文件有创建，则删除文件
            if (affectedCount == 1) try {
                storageService.delete(request.sha256!!, storageCredentials)
            } catch (exception: Exception) {
                logger.error("Failed to delete new created file[${request.sha256}]", exception)
            }
            // 异常往上抛
            throw exception
        }
    }

    /**
     * 加载ArtifactInputStream
     * 如果node为null，则返回null
     * 如果为head请求则返回empty input stream
     */
    @Deprecated("NodeInfo移除后此方法也会移除")
    fun loadArtifactInputStream(
        node: NodeInfo?,
        storageCredentials: StorageCredentials?
    ): ArtifactInputStream? {
        if (node == null || node.folder) {
            return null
        }
        val request = getRequestOrNull()
        val range = try {
            request?.let { resolveRange(it, node.size) } ?: Range.full(node.size)
        } catch (exception: IllegalArgumentException) {
            logger.warn("Failed to resolve http range: ${exception.message}")
            throw ErrorCodeException(
                status = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                messageCode = CommonMessageCode.REQUEST_RANGE_INVALID
            )
        }
        if (range.isEmpty() || request?.method == HttpMethod.HEAD.name) {
            return ArtifactInputStream(EmptyInputStream.INSTANCE, range)
        }
        val sha256 = node.sha256.orEmpty()
        /*
        * 顺序查找
        * 1.当前仓库存储实例 (正常情况)
        * 2.中央节点拉取（边缘节点情况）
        * 3.拷贝存储实例（节点快速拷贝场景）
        * 4.旧存储实例（仓库迁移场景）
        * */
        return storageService.load(sha256, range, storageCredentials)
            ?: loadFromCenterIfNecessary(sha256, range, storageCredentials?.key)
            ?: loadFromCopyIfNecessary(node, range)
            ?: loadFromRepoOldIfNecessary(node, range, storageCredentials)
    }

    /**
     * 加载ArtifactInputStream
     * 如果node为null，则返回null
     * 如果为head请求则返回empty input stream
     */
    fun loadArtifactInputStream(
        node: NodeDetail?,
        storageCredentials: StorageCredentials?
    ): ArtifactInputStream? {
        return loadArtifactInputStream(node?.nodeInfo, storageCredentials)
    }

    /**
     * 因为支持快速copy，也就是说源节点的数据可能还未完全上传成功，
     * 还在本地文件系统上，这时拷贝节点就会从源存储去加载数据。
     * */
    private fun loadFromCopyIfNecessary(
        node: NodeInfo,
        range: Range
    ): ArtifactInputStream? {
        node.copyFromCredentialsKey?.let {
            val digest = node.sha256!!
            logger.info("load data [$digest] from copy credentialsKey [$it]")
            val fromCredentialsKey = storageCredentialsClient.findByKey(it).data
            return storageService.load(digest, range, fromCredentialsKey)
        }
        return null
    }

    /**
     * 仓库迁移场景
     * 仓库还在迁移中，旧的数据还未存储到新的存储实例上去，所以从仓库之前的存储实例中加载
     * */
    private fun loadFromRepoOldIfNecessary(
        node: NodeInfo,
        range: Range,
        storageCredentials: StorageCredentials?
    ): ArtifactInputStream? {
        val repositoryDetail = getRepoDetail(node)
        val oldCredentials = findStorageCredentialsByKey(repositoryDetail.oldCredentialsKey)
        if (storageCredentials != oldCredentials) {
            logger.info(
                "load data [${node.sha256!!}] from" +
                    " repo old credentialsKey [${repositoryDetail.oldCredentialsKey}]"
            )
            return storageService.load(node.sha256!!, range, oldCredentials)
        }
        return null
    }

    /**
     * 根据credentialsKey查找StorageCredentials
     * */
    private fun findStorageCredentialsByKey(credentialsKey: String?): StorageCredentials? {
        credentialsKey ?: return null
        return storageCredentialsClient.findByKey(credentialsKey).data
    }

    /**
     * 通过中心节点代理拉取文件
     */
    private fun loadFromCenterIfNecessary(
        sha256: String,
        range: Range,
        storageKey: String?
    ): ArtifactInputStream? {
        if (clusterProperties.role == RoleType.CENTER || !existInCenter(sha256, storageKey)) {
            return null
        }
        return loadFromCenter(sha256, range, storageKey)
    }

    /**
     * 判断数据在中心节点是否存在
     * 请求异常返回false
     */
    private fun existInCenter(sha256: String, storageKey: String?): Boolean {
        return try {
            blobReplicaClient.check(sha256, storageKey).data ?: false
        } catch (exception: Exception) {
            logger.error("Failed to check blob data[$sha256] in center node.", exception)
            false
        }
    }

    /**
     * 通过中心节点代理拉取文件
     */
    private fun loadFromCenter(
        sha256: String,
        range: Range,
        storageKey: String?
    ): ArtifactInputStream? {
        try {
            val request = BlobPullRequest(sha256, range, storageKey)
            val response = blobReplicaClient.pull(request)
            check(response.status() == HttpStatus.OK.value) {
                "Failed to pull blob[$sha256] from center node, status: ${response.status()}"
            }
            val artifactInputStream = response.body()?.asInputStream()?.artifactStream(range)
            if (artifactInputStream != null && range.isFullContent()) {
                val listener = ProxyBlobCacheWriter(storageService, sha256)
                artifactInputStream.addListener(listener)
            }
            logger.info("Pull blob data[$sha256] from center node.")
            return artifactInputStream
        } catch (exception: Exception) {
            logger.error("Failed to pull blob data[$sha256] from center node.", exception)
        }
        return null
    }

    /**
     * 获取RepoDetail
     * */
    private fun getRepoDetail(node: NodeInfo): RepositoryDetail {
        with(node) {
            // 如果当前上下文存在该node的repo信息则，返回上下文中的repo，大部分请求应该命中这
            ArtifactContextHolder.getRepoDetail()?.let {
                if (it.projectId == projectId && it.name == name) {
                    return it
                }
            }
            // 如果是异步或者请求上下文找不到，则通过查询，并进行缓存
            val repositoryId = ArtifactContextHolder.RepositoryId(
                projectId = projectId,
                repoName = repoName
            )
            return ArtifactContextHolder.getRepoDetail(repositoryId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageManager::class.java)
    }
}
