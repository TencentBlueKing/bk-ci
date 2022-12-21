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

package com.tencent.bkrepo.job.batch

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.job.COUNT
import com.tencent.bkrepo.job.CREDENTIALS
import com.tencent.bkrepo.job.ID
import com.tencent.bkrepo.job.SHARDING_COUNT
import com.tencent.bkrepo.job.batch.base.FileJobContext
import com.tencent.bkrepo.job.batch.base.MongoDbBatchJob
import com.tencent.bkrepo.job.batch.base.JobContext
import com.tencent.bkrepo.job.config.MongodbJobProperties
import com.tencent.bkrepo.job.exception.JobExecuteException
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 清理引用=0的文件
 */
class FileReferenceCleanupJob(
    private val storageService: StorageService,
    private val mongoTemplate: MongoTemplate,
    private val storageCredentialsClient: StorageCredentialsClient,
    properties: MongodbJobProperties
) : MongoDbBatchJob<FileReferenceCleanupJob.FileReferenceData>(properties) {

    @Scheduled(cron = "0 0 4/6 * * ?") // 4点开始，6小时执行一次
    override fun start(): Boolean {
        return super.start()
    }

    override fun createJobContext(): JobContext {
        return FileJobContext()
    }

    override fun entityClass(): Class<FileReferenceData> {
        return FileReferenceData::class.java
    }

    override fun collectionNames(): List<String> {
        return (0 until SHARDING_COUNT)
            .map { "$COLLECTION_NAME_PREFIX$it" }
            .toList()
    }

    override fun buildQuery(): Query {
        return Query(Criteria.where(COUNT).isEqualTo(0))
    }

    override fun run(row: FileReferenceData, collectionName: String, context: JobContext) {
        val credentialsKey = row.credentialsKey
        val sha256 = row.sha256
        val id = row.id
        val storageCredentials = credentialsKey?.let { getCredentials(credentialsKey) }
        try {
            if (sha256.isNotBlank() && storageService.exist(sha256, storageCredentials)) {
                storageService.delete(sha256, storageCredentials)
            } else {
                (context as FileJobContext).fileMissing.incrementAndGet()
                logger.warn("File[$sha256] is missing on [$storageCredentials], skip cleaning up.")
            }
            mongoTemplate.remove(Query(Criteria(ID).isEqualTo(id)), collectionName)
        } catch (e: Exception) {
            throw JobExecuteException("Failed to delete file[$sha256] on [$storageCredentials].", e)
        }
    }

    override fun getLockAtMostFor(): Duration = Duration.ofDays(7)

    private fun getCredentials(key: String): StorageCredentials? {
        return cacheMap.getOrPut(key) {
            storageCredentialsClient.findByKey(key).data ?: return null
        }
    }

    private val cacheMap: ConcurrentHashMap<String, StorageCredentials?> = ConcurrentHashMap()

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val COLLECTION_NAME_PREFIX = "file_reference_"
    }

    data class FileReferenceData(private val map: Map<String, Any?>) {
        val id: String? by map
        val sha256: String by map
        val credentialsKey: String? = map[CREDENTIALS] as String?
    }

    override fun mapToObject(row: Map<String, Any?>): FileReferenceData {
        return FileReferenceData(row)
    }
}
