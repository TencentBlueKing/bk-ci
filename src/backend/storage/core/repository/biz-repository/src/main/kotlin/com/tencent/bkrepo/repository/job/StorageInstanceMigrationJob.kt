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
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
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
    private val storageProperties: StorageProperties,
    private val taskAsyncExecutor: ThreadPoolTaskExecutor
) {

    fun migrate(projectId: String, repoName: String, dstStorageKey: String) {
        logger.info("Start to migrate storage, projectId: $projectId, repoName: $repoName, dst key: $dstStorageKey.")
        val context = checkAndBuildContext(projectId, repoName, dstStorageKey)
        taskAsyncExecutor.submit {
            try {
                // 修改repository配置，保证之后上传的文件直接保存到新存储实例中，文件下载时，当前实例找不到的情况下会去默认存储找
                repositoryService.updateStorageCredentialsKey(projectId, repoName, dstStorageKey)
                // 迁移老文件
                migrateOldFile(context)
                // 校验新文件
                correctNewFile(context)
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
            val newFileNodeList = nodeDao.find(query)
            logger.info("[${newFileNodeList.size}] new created file nodes to be checked.")
            newFileNodeList.forEach { node ->
                val sha256 = node.sha256.orEmpty()
                try {
                    correctNode(context, node)
                } catch (exception: Exception) {
                    logger.error("Failed to check file[$sha256].", exception)
                    correctFailedCount += 1
                } finally {
                    correctTotalCount += 1
                }
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
                correctMigrateCount += 1
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
                correctSuccessCount += 1
                logger.info("Success to correct file[$sha256].")
            } else {
                // dst和src都不存在，可能还在CFS上或者数据丢失，error
                dataMissingCount += 1
                throw IllegalStateException("File data [$sha256] not found in src and dst")
            }
        }
    }

    /**
     * 迁移老文件
     */
    private fun migrateOldFile(context: MigrationContext) {
        with(context) {
            // 分页查询文件节点，只查询当前时间以前创建的文件节点，之后创建的是在新实例上
            val query = buildOldNodeQuery(this)
            val total = nodeDao.count(query)
            logger.info("$total records to be migrated totally.")
            var page = 0
            var nodeList = nodeDao.find(query.with(PageRequest.of(page, PAGE_SIZE)))
            while (nodeList.isNotEmpty()) {
                logger.info("Retrieved ${nodeList.size} records to migrate, progress: $totalCount/$total.")
                nodeList.forEach { node -> migrateNode(this, node) }
                page += 1
                nodeList = nodeDao.find(query.with(PageRequest.of(page, PAGE_SIZE)))
            }
            assert(total == totalCount) { "$totalCount has been migrated, while $total needs to be migrate." }
            val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
            logger.info(
                "Complete migrate old files, project: $projectId, repo: $repoName, key: $dstStorageKey, " +
                    "total: $totalCount, success: $successCount, failed: $failedCount, duration $durationSeconds s."
            )
        }
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
                successCount += 1
            } catch (exception: Exception) {
                logger.error("Failed to migrate file[$sha256].", exception)
                failedCount += 1
            } finally {
                totalCount += 1
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
            ).with(Sort.by(Sort.Direction.DESC, TNode::createdDate.name))
        }
    }

    private fun buildNewNodeQuery(context: MigrationContext): Query {
        with(context) {
            return Query.query(
                where(TNode::projectId).isEqualTo(projectId)
                    .and(TNode::repoName).isEqualTo(repoName)
                    .and(TNode::folder).isEqualTo(false)
                    .and(TNode::createdDate).gt(startTime)
            ).with(Sort.by(Sort.Direction.DESC, TNode::createdDate.name))
        }
    }

    private fun checkAndBuildContext(projectId: String, repoName: String, dstStorageKey: String): MigrationContext {
        val repository = repositoryService.getRepoDetail(projectId, repoName)
            ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
        // 限制只能由默认storage迁移
        val srcStorageKey = repository.storageCredentials?.key
        if (srcStorageKey != null) {
            throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Only support migrate from default storage")
        }
        val srcStorageCredentials = storageProperties.defaultStorageCredentials()
        val dstStorageCredentials = storageCredentialService.findByKey(dstStorageKey)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, dstStorageKey)
        // 限制存储实例类型必须相同且为InnerCos
        if (srcStorageCredentials !is InnerCosCredentials || dstStorageCredentials !is InnerCosCredentials) {
            throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Only support inner cos storage")
        }

        val srcBucket = srcStorageCredentials.bucket
        val srcCosClient = CosClient(srcStorageCredentials)
        val dstCosClient = CosClient(dstStorageCredentials)
        val startTime = LocalDateTime.now()
        return MigrationContext(
            projectId = projectId,
            repoName = repoName,
            srcStorageKey = srcStorageKey,
            dstStorageKey = dstStorageKey,
            srcBucket = srcBucket,
            srcCosClient = srcCosClient,
            dstCosClient = dstCosClient,
            startTime = startTime
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
        var successCount: Long = 0L,
        var failedCount: Long = 0L,
        var totalCount: Long = 0L,
        var correctSuccessCount: Long = 0L,
        var correctMigrateCount: Long = 0L,
        var dataMissingCount: Long = 0L,
        var correctFailedCount: Long = 0L,
        var correctTotalCount: Long = 0L
    )
}
