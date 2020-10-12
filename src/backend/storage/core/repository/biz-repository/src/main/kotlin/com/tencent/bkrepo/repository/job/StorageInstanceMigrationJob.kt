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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
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
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.repository.RepoRepository
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.service.FileReferenceService
import com.tencent.bkrepo.repository.service.RepositoryService
import com.tencent.bkrepo.repository.service.StorageCredentialService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import javax.annotation.Resource

/**
 * 存储实例迁移任务
 */
@Component
class StorageInstanceMigrationJob {

    @Autowired
    private lateinit var nodeDao: NodeDao

    @Autowired
    private lateinit var repositoryService: RepositoryService

    @Autowired
    private lateinit var repoRepository: RepoRepository

    @Autowired
    private lateinit var fileReferenceService: FileReferenceService

    @Autowired
    private lateinit var storageCredentialService: StorageCredentialService

    @Autowired
    private lateinit var storageProperties: StorageProperties

    @Resource
    lateinit var taskAsyncExecutor: ThreadPoolTaskExecutor

    fun migrate(projectId: String, repoName: String, destStorageKey: String) {
        logger.info("Start to migrate storage instance, projectId: $projectId, repoName: $repoName, dest key: $destStorageKey.")
        val repository = repositoryService.queryRepository(projectId, repoName)
            ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
        // 限制只能由默认storage迁移
        val srcStorageKey = repository.credentialsKey
        if (srcStorageKey != null) {
            throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED)
        }
        val srcStorageCredentials = storageProperties.defaultStorageCredentials()
        val destStorageCredentials = storageCredentialService.findByKey(destStorageKey)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, destStorageKey)
        // 限制存储实例类型必须相同且为InnerCos
        if (srcStorageCredentials !is InnerCosCredentials || destStorageCredentials !is InnerCosCredentials) {
            throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED)
        }

        val srcBucket = srcStorageCredentials.bucket
        val srcCosClient = CosClient(srcStorageCredentials)
        val destCosClient = CosClient(destStorageCredentials)
        val startTime = LocalDateTime.now()

        taskAsyncExecutor.submit {
            try {
                // 修改repository配置，保证之后上传的文件直接保存到新存储实例中，文件下载时，当前实例找不到的情况下会去默认存储找
                repository.credentialsKey = destStorageKey
                repoRepository.save(repository)

                // 迁移老文件
                migrateOldFile(projectId, repoName, startTime, srcBucket, srcStorageKey, destStorageKey, srcCosClient, destCosClient)

                // 校验新文件
                checkNewFile(projectId, repoName, startTime, srcBucket, srcStorageKey, destStorageKey, srcCosClient, destCosClient)
            } catch (exception: RuntimeException) {
                logger.error("Migrate storage instance failed.", exception)
            }
        }
    }

    private fun checkNewFile(
        projectId: String,
        repoName: String,
        startTime: LocalDateTime,
        srcBucket: String,
        srcStorageKey: String? = null,
        destStorageKey: String,
        srcCosClient: CosClient,
        destCosClient: CosClient
    ) {
        var correctCount = 0L
        var migrateCount = 0L
        var dataMissingCount = 0L
        var failedCount = 0L
        var totalCount = 0L
        // 查询新上传文件
        val newFileNodeQuery = Query.query(
            Criteria.where(TNode::projectId.name).`is`(projectId)
                .and(TNode::repoName.name).`is`(repoName)
                .and(TNode::folder.name).`is`(false)
                .and(TNode::createdDate.name).gt(startTime)
        ).with(Sort.by(Sort.Direction.DESC, TNode::createdDate.name))
        val newFileNodeList = nodeDao.find(newFileNodeQuery)
        logger.info("${newFileNodeList.size} new created file nodes to be checked.")
        newFileNodeList.forEach { node ->
            val sha256 = node.sha256!!
            try {
                // 检查
                if (!checkDataExist(sha256, destCosClient)) {
                    // dest data不存在，说明数据有问题
                    if (checkReferenceExist(sha256, destStorageKey)) {
                        // dest reference存在
                        if (checkDataExist(sha256, srcCosClient)) {
                            // dest不存在但src data存在, 说明数据被落到旧存储实例中
                            destCosClient.copyObject(CopyObjectRequest(srcBucket, sha256, sha256))
                            correctCount += 1
                            logger.info("Success to correct file[$sha256].")
                        } else {
                            // dest和src都不存在，可能还在CFS上或者数据丢失，error
                            dataMissingCount += 1
                            throw IllegalStateException("File data [$sha256] not found in src and dest")
                        }
                    } else {
                        // dest data和reference都不存在，migrate
                        migrateNode(node, srcBucket, srcStorageKey, destStorageKey, srcCosClient, destCosClient)
                        migrateCount += 1
                        logger.info("Success to migrate file[$sha256].")
                    }
                }
            } catch (exception: RuntimeException) {
                logger.error("Failed to check file[$sha256].", exception)
                failedCount += 1
            } finally {
                totalCount += 1
            }
        }

        val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
        logger.info(
            "Complete check new created files, projectId: $projectId, repoName: $repoName, key: $destStorageKey, " +
                "total: $totalCount, correct: $correctCount, migrate: $migrateCount, missing data: $dataMissingCount, " +
                "failed: $failedCount, duration $durationSeconds s totally."
        )
    }

    private fun migrateOldFile(
        projectId: String,
        repoName: String,
        startTime: LocalDateTime,
        srcBucket: String,
        srcStorageKey: String? = null,
        destStorageKey: String,
        srcCosClient: CosClient,
        destCosClient: CosClient
    ) {
        var successCount = 0L
        var failedCount = 0L
        var totalCount = 0L

        // 分页查询文件节点，只查询当前时间以前创建的文件节点，之后创建的是在新实例上
        var page = 0
        val size = 10000
        val query = Query.query(
            Criteria.where(TNode::projectId.name).`is`(projectId)
                .and(TNode::repoName.name).`is`(repoName)
                .and(TNode::folder.name).`is`(false)
                .and(TNode::createdDate.name).lte(startTime)
        ).with(Sort.by(Sort.Direction.DESC, TNode::createdDate.name))

        val total = nodeDao.count(query)
        logger.info("$total records to be migrated totally.")

        query.with(PageRequest.of(page, size))
        var nodeList = nodeDao.find(query)
        while (nodeList.isNotEmpty()) {
            logger.info("Retrieved ${nodeList.size} records to migrate, progress: $totalCount/$total.")
            nodeList.forEach { node ->
                // 迁移数据，直接操作cos
                try {
                    migrateNode(node, srcBucket, srcStorageKey, destStorageKey, srcCosClient, destCosClient)
                    logger.info("Success to migrate file[${node.sha256}].")
                    successCount += 1
                } catch (exception: RuntimeException) {
                    logger.error("Failed to migrate file[${node.sha256}].", exception)
                    failedCount += 1
                } finally {
                    totalCount += 1
                }
            }
            page += 1
            query.with(PageRequest.of(page, size))
            nodeList = nodeDao.find(query)
        }
        val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
        logger.info(
            "Complete migrate old files, projectId: $projectId, repoName: $repoName, key: $destStorageKey, " +
                "total: $totalCount, success: $successCount, failed: $failedCount, duration $durationSeconds s totally."
        )
        assert(total == totalCount) { "$totalCount has been migrated, while $total needs to be migrate." }
    }

    private fun checkDataExist(sha256: String, cosClient: CosClient): Boolean {
        return cosClient.checkObjectExist(CheckObjectExistRequest(sha256))
    }

    private fun checkReferenceExist(sha256: String, storageKey: String): Boolean {
        return fileReferenceService.count(sha256, storageKey) > 0
    }

    private fun migrateNode(
        node: TNode,
        srcBucket: String,
        srcStorageKey: String? = null,
        destStorageKey: String,
        srcCosClient: CosClient,
        destCosClient: CosClient
    ) {
        val sha256 = node.sha256!!
        // 判断是否存在
        if (!srcCosClient.checkObjectExist(CheckObjectExistRequest(sha256))) {
            throw IllegalStateException("File data [$sha256] not found in src.")
        }
        // 跨bucket copy
        destCosClient.copyObject(CopyObjectRequest(srcBucket, sha256, sha256))
        // old引用计数 -1
        if (!fileReferenceService.decrement(sha256, srcStorageKey)) {
            throw IllegalStateException("Failed to decrement file reference[$sha256].")
        }
        // new引用计数 +1
        if (!fileReferenceService.increment(sha256, destStorageKey)) {
            throw IllegalStateException("Failed to increment file reference[$sha256].")
        }
        // FileReferenceCleanupJob 会定期清理引用为0的文件数据，所以不需要删除文件数据
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
