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

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.constant.SHARDING_COUNT
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.job.base.CenterNodeJob
import com.tencent.bkrepo.repository.model.TFileReference
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 清理引用=0的文件
 */
@Component
class FileReferenceCleanupJob(
    private val fileReferenceDao: FileReferenceDao,
    private val storageService: StorageService,
    private val storageCredentialService: StorageCredentialService,
    private val mongoTemplate: MongoTemplate = fileReferenceDao.determineMongoTemplate()
) : CenterNodeJob() {

//    @Scheduled(cron = "0 0 4/6 * * ?") // 4点开始，6小时执行一次
    override fun start() {
        super.start()
    }

    override fun run() {
        val context = JobContext()
        with(context) {
            for (sequence in 0 until SHARDING_COUNT) {
                cleanupCollection(sequence, context)
            }
            logger.info(
                "Clean up [$total] zero reference files, success[$success], " +
                    "failed[$failed], file missing[$fileMissing]."
            )
        }
    }

    override fun getLockAtMostFor(): Duration = Duration.ofDays(7)

    private fun cleanupCollection(sequence: Int, context: JobContext) {
        val query = Query.query(TFileReference::count.isEqualTo(0))
        val collectionName = fileReferenceDao.parseSequenceToCollectionName(sequence)
        val zeroReferenceList = mongoTemplate.find(query, TFileReference::class.java, collectionName)
        zeroReferenceList.forEach {
            val storageCredentials = getCredentials(it.credentialsKey, context.cacheMap)
            try {
                if (it.sha256.isNotBlank() && storageService.exist(it.sha256, storageCredentials)) {
                    storageService.delete(it.sha256, storageCredentials)
                    context.success += 1
                } else {
                    context.fileMissing += 1
                    logger.warn("File[${it.sha256}] is missing on [$storageCredentials], skip cleaning up.")
                }
                mongoTemplate.remove(it, collectionName)
            } catch (ignored: Exception) {
                context.failed += 1
                logger.error("Failed to delete file[${it.sha256}] on [$storageCredentials].", ignored)
            } finally {
                context.total += 1
            }
        }
    }

    private fun getCredentials(key: String?, cacheMap: MutableMap<String, StorageCredentials>): StorageCredentials? {
        return key?.let {
            if (cacheMap[it] == null) {
                storageCredentialService.findByKey(key)!!.apply { cacheMap[it] = this }
            }
            cacheMap[it]
        }
    }

    data class JobContext(
        val cacheMap: MutableMap<String, StorageCredentials> = mutableMapOf(),
        var total: Long = 0L,
        var success: Long = 0L,
        var failed: Long = 0L,
        var fileMissing: Long = 0L
    )

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
