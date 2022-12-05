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

package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.kotlin.adapter.util.CompletableFutureKotlin
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.innercos.client.CosClient
import com.tencent.bkrepo.common.storage.innercos.request.CheckObjectExistRequest
import com.tencent.bkrepo.common.storage.innercos.request.CopyObjectRequest
import com.tencent.bkrepo.common.storage.innercos.retry
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.service.file.FileReferenceService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * 存储实例迁移任务
 */
// TooGenericExceptionCaught: 迁移过程无法预知具体异常类型
@Suppress("TooGenericExceptionCaught")
@Component
class StorageInstanceMigrationJob(
    private val nodeDao: NodeDao,
    private val repositoryService: RepositoryService,
    private val fileReferenceService: FileReferenceService,
    private val storageCredentialService: StorageCredentialService,
    private val taskAsyncExecutor: ThreadPoolTaskExecutor,
    private val storageProperties: StorageProperties
) {

    fun migrate(
        projectId: String,
        repoName: String,
        dstStorageKey: String,
        failedPointId: String? = null,
        skipPage: Int? = null,
        preStartTime: LocalDateTime? = null
    ) {
        logger.info("Start to migrate storage, projectId: $projectId, repoName: $repoName, dst key: $dstStorageKey.")
        val context = checkAndBuildContext(projectId, repoName, dstStorageKey, failedPointId, skipPage, preStartTime)
        taskAsyncExecutor.submit {
            try {
                // 修改repository配置，保证之后上传的文件直接保存到新存储实例中，文件下载时，当前实例找不到的情况下会去默认存储找
                failedPointId ?: repositoryService.updateStorageCredentialsKey(projectId, repoName, dstStorageKey)
                // 迁移老文件
                if (migrateOldFile(context)) {
                    // 校验新文件
                    correctNewFile(context)
                }
            } catch (exception: Exception) {
                logger.error("Migrate storage instance failed.", exception)
            }
        }
    }

    private fun correctNewFile(context: MigrationContext) {
        with(context) {
            // 查询新上传文件
            val correctStartTime = LocalDateTime.now()
            val query = buildNewNodeQuery(context)
            val total = nodeDao.count(query)
            logger.info("[$total] new created file nodes to be checked.")
            var page = 0
            var newFileNodeList = nodeDao.find(query.with(PageRequest.of(page, PAGE_SIZE)))
            while (newFileNodeList.isNotEmpty()) {
                logger.info(
                    "Retrieved ${newFileNodeList.size} records to migrate, " +
                            "progress: $correctTotalCount/$total."
                )
                val futures = newFileNodeList.map { node ->
                    CompletableFuture.runAsync {
                        val sha256 = node.sha256.orEmpty()
                        try {
                            correctNode(context, node)
                        } catch (exception: Exception) {
                            logger.error("Failed to check file[$sha256].", exception)
                            correctFailedCount.incrementAndGet()
                        } finally {
                            correctTotalCount.incrementAndGet()
                        }
                    }
                }.toTypedArray()
                CompletableFutureKotlin.allOf(futures).join()
                newFileNodeList = nodeDao.find(query.with(PageRequest.of(++page, PAGE_SIZE)))
            }
            val durationSeconds = Duration.between(correctStartTime, LocalDateTime.now()).seconds
            logger.info(
                "Complete check new created files, projectId: $projectId, repoName: $repoName, key: $dstStorageKey, " +
                        "total: $correctTotalCount, correct: $correctSuccessCount, migrate: $correctMigrateCount, " +
                        "missing data: $dataMissingCount, failed: $correctFailedCount, duration $durationSeconds s."
            )
        }
    }

    private fun correctNode(context: MigrationContext, node: TNode) {
        with(context) {
            val sha256 = node.sha256.orEmpty()
            // dst data存在，说明数据没问题，跳过处理
            if (dstCosClient.checkObjectExist(CheckObjectExistRequest(sha256))) {
                return
            }
            if (fileReferenceService.count(sha256, dstStorageKey) > 0) {
                correct(this, sha256)
            } else {
                // dst data和reference都不存在，migrate
                migrate(this, sha256)
                correctMigrateCount.incrementAndGet()
                logger.info("Success to migrate file[$sha256].")
            }
        }
    }

    private fun correct(context: MigrationContext, sha256: String) {
        with(context) {
            // dst reference存在
            if (srcCosClient.checkObjectExist(CheckObjectExistRequest(sha256))) {
                // dst不存在但src data存在, 说明数据被落到旧存储实例中
                retry(RETRY_COUNT) {
                    dstCosClient.copyObject(CopyObjectRequest(srcBucket, sha256, sha256))
                }
                correctSuccessCount.incrementAndGet()
                logger.info("Success to correct file[$sha256].")
            } else {
                // dst和src都不存在，可能还在CFS上或者数据丢失，error
                dataMissingCount.incrementAndGet()
                throw IllegalStateException("File data [$sha256] not found in src and dst")
            }
        }
    }

    /**
     * 迁移老文件
     */
    private fun migrateOldFile(context: MigrationContext): Boolean {
        with(context) {
            // 分页查询文件节点，只查询当前时间以前创建的文件节点，之后创建的是在新实例上
            val query = buildOldNodeQuery(this)
            val total = nodeDao.count(query)
            logger.info("$total records to be migrated totally.")
            var page = skipPage ?: 0
            var nodeList = nodeDao.find(query.with(PageRequest.of(page, PAGE_SIZE)))
            failedPointId?.let {
                val (nodes, node) = findContinueSubNodeList(failedPointId, nodeList)
                node ?: let {
                    logger.info("not found continue point,stop migrate repository")
                    return false
                }
                nodeList = if (nodes.isEmpty())
                    nodeDao.find(query.with(PageRequest.of(page++, PAGE_SIZE))) else nodes

                val initTotal = ((skipPage ?: 0) * PAGE_SIZE).toLong().plus(PAGE_SIZE - nodes.size)
                totalCount = AtomicLong(initTotal)
                logger.info(
                    "start continue migrate repository [$repoName]," +
                            "previous node id $failedPointId,sha256 ${node.sha256}"
                )
            }
            while (nodeList.isNotEmpty()) {
                logger.info("Retrieved ${nodeList.size} records to migrate, progress: $totalCount/$total.")
                val futures = nodeList.map { node ->
                    CompletableFuture.runAsync {
                        migrateNode(this, node)
                    }
                }.toTypedArray()
                CompletableFutureKotlin.allOf(futures).join()
                page += 1
                nodeList = nodeDao.find(query.with(PageRequest.of(page, PAGE_SIZE)))
            }
            val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
            logger.info(
                "Complete migrate old files, project: $projectId, repo: $repoName, key: $dstStorageKey, " +
                        "total: $totalCount, success: $successCount, failed: $failedCount, duration $durationSeconds s."
            )
            return true
        }
    }

    private fun findContinueSubNodeList(failedPoint: String?, nodes: List<TNode>): Pair<List<TNode>, TNode?> {
        var node: TNode? = null
        nodes.forEachIndexed { index, tNode ->
            if (tNode.id == failedPoint) {
                node = tNode
                if (index == nodes.lastIndex) {
                    return Pair(emptyList(), node)
                }
                return Pair(nodes.subList(index + 1, nodes.size), node)
            }

        }
        return Pair(emptyList(), node)
    }

    /**
     * 迁移数据，直接操作cos
     */
    private fun migrateNode(context: MigrationContext, node: TNode) {
        with(context) {
            val sha256 = node.sha256.orEmpty()
            try {
                migrate(this, sha256)
                logger.info("Success to migrate file[$sha256].")
                successCount.incrementAndGet()
            } catch (exception: Exception) {
                logger.error("Failed to migrate file[$sha256].", exception)
                failedCount.incrementAndGet()
            } finally {
                totalCount.incrementAndGet()
            }
        }
    }

    private fun migrate(context: MigrationContext, sha256: String) {
        with(context) {
            // 判断是否存在
            check(srcCosClient.checkObjectExist(CheckObjectExistRequest(sha256))) {
                "File data [$sha256] not found in src."
            }
            // 跨bucket copy
            retry(RETRY_COUNT) {
                dstCosClient.copyObject(CopyObjectRequest(srcBucket, sha256, sha256))
            }
            // old引用计数 -1
            check(fileReferenceService.decrement(sha256, srcStorageKey)) {
                "Failed to decrement file reference[$sha256]."
            }
            // new引用计数 +1
            check(fileReferenceService.increment(sha256, dstStorageKey)) {
                "Failed to decrement file reference[$sha256]."
            }
            // FileReferenceCleanupJob 会定期清理引用为0的文件数据，所以不需要删除文件数据
        }
    }

    private fun buildOldNodeQuery(context: MigrationContext): Query {
        with(context) {
            return Query.query(
                where(TNode::projectId).isEqualTo(projectId)
                    .and(TNode::repoName).isEqualTo(repoName)
                    .and(TNode::folder).isEqualTo(false)
                    .and(TNode::createdDate).lte(startTime)
            ).with(Sort.by(Sort.Direction.DESC, TNode::id.name))
        }
    }

    private fun buildNewNodeQuery(context: MigrationContext): Query {
        with(context) {
            return Query.query(
                where(TNode::projectId).isEqualTo(projectId)
                    .and(TNode::repoName).isEqualTo(repoName)
                    .and(TNode::folder).isEqualTo(false)
                    .and(TNode::createdDate).gt(startTime)
            ).with(Sort.by(Sort.Direction.DESC, TNode::id.name))
        }
    }

    private fun checkAndBuildContext(
        projectId: String, repoName: String, dstStorageKey: String, failedPointId: String? = null,
        skipPage: Int? = null, preStartTime: LocalDateTime? = null
    ): MigrationContext {
        val repository = repositoryService.getRepoDetail(projectId, repoName)
            ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)

        val srcStorageKey: String?
        val srcStorageCredentials: StorageCredentials
        val dstStorageCredentials: StorageCredentials
        if (failedPointId != null) {
            srcStorageKey = repository.oldCredentialsKey
            srcStorageCredentials = if (srcStorageKey == null) storageProperties.defaultStorageCredentials()
                else storageCredentialService.findByKey(srcStorageKey) ?: throw ErrorCodeException(
                    CommonMessageCode.RESOURCE_NOT_FOUND,
                    srcStorageKey
                )

            dstStorageCredentials = repository.storageCredentials ?: throw ErrorCodeException(
                CommonMessageCode.RESOURCE_NOT_FOUND,
                dstStorageKey
            )
            logger.info("continue migrate src key $srcStorageKey ,dst key $dstStorageKey")
        } else {
            srcStorageKey = repository.storageCredentials?.key
            srcStorageCredentials = repository.storageCredentials ?: storageProperties.defaultStorageCredentials()
            dstStorageCredentials = storageCredentialService.findByKey(dstStorageKey)
                ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, dstStorageKey)
        }
        if (srcStorageCredentials == dstStorageCredentials) {
            throw ErrorCodeException(
                CommonMessageCode.METHOD_NOT_ALLOWED,
                "Src and Dst storageCredentials are same"
            )
        }
        // 限制存储实例类型必须相同且为InnerCos
        if (srcStorageCredentials !is InnerCosCredentials || dstStorageCredentials !is InnerCosCredentials) {
            throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Only support inner cos storage")
        }

        val srcBucket = srcStorageCredentials.bucket
        val srcCosClient = CosClient(srcStorageCredentials)
        val dstCosClient = CosClient(dstStorageCredentials)
        val startTime = preStartTime ?: LocalDateTime.now()
        return MigrationContext(
            projectId = projectId,
            repoName = repoName,
            srcStorageKey = srcStorageKey,
            dstStorageKey = dstStorageKey,
            srcBucket = srcBucket,
            srcCosClient = srcCosClient,
            dstCosClient = dstCosClient,
            startTime = startTime,
            skipPage = skipPage,
            failedPointId = failedPointId
        )
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val PAGE_SIZE = 10000
        private const val RETRY_COUNT = 3
    }

    data class MigrationContext(
        val projectId: String,
        val repoName: String,
        val srcStorageKey: String? = null,
        val dstStorageKey: String,
        val srcBucket: String,
        val srcCosClient: CosClient,
        val dstCosClient: CosClient,
        val startTime: LocalDateTime,
        var successCount: AtomicLong = AtomicLong(),
        var failedCount: AtomicLong = AtomicLong(),
        var totalCount: AtomicLong = AtomicLong(),
        var correctSuccessCount: AtomicLong = AtomicLong(),
        var correctMigrateCount: AtomicLong = AtomicLong(),
        var dataMissingCount: AtomicLong = AtomicLong(),
        var correctFailedCount: AtomicLong = AtomicLong(),
        var correctTotalCount: AtomicLong = AtomicLong(),
        val failedPointId: String? = null,
        val skipPage: Int? = null
    )
}
