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
import com.tencent.bkrepo.repository.model.TFileReference
import com.tencent.bkrepo.repository.service.StorageCredentialService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 清理引用=0的文件
 */
@Component
class FileReferenceCleanupJob(
    private val fileReferenceDao: FileReferenceDao,
    private val storageService: StorageService,
    private val storageCredentialService: StorageCredentialService
) {

    @Scheduled(cron = "0 0 4/6 * * ?") // 4点开始，6小时执行一次
    @SchedulerLock(name = "FileReferenceCleanupJob", lockAtMostFor = "PT6H")
    fun cleanup() {
        logger.info("Starting to clean up file reference.")
        var totalCount = 0L
        var cleanupCount = 0L
        var failedCount = 0L
        var fileMissingCount = 0L
        val storageCredentialsMap = mutableMapOf<String, StorageCredentials>()
        val startTimeMillis = System.currentTimeMillis()
        val query = Query.query(TFileReference::count.isEqualTo(0))
        val mongoTemplate = fileReferenceDao.determineMongoTemplate()
        for (sequence in 0 until SHARDING_COUNT) {
            val collectionName = fileReferenceDao.parseSequenceToCollectionName(sequence)
            val zeroReferenceList = mongoTemplate.find(query, TFileReference::class.java, collectionName)
            zeroReferenceList.forEach {
                val storageCredentials = it.credentialsKey?.let { key ->
                    storageCredentialsMap[key] ?: run {
                        storageCredentialService.findByKey(key)!!.apply { storageCredentialsMap[key] = this }
                    }
                }
                try {
                    if (it.sha256.isNotBlank() && storageService.exist(it.sha256, storageCredentials)) {
                        storageService.delete(it.sha256, storageCredentials)
                        cleanupCount += 1
                    } else {
                        logger.warn("File[${it.sha256}] is missing on [$storageCredentials], skip cleaning up.")
                        fileMissingCount += 1
                    }
                    fileReferenceDao.determineMongoTemplate().remove(it, collectionName)
                } catch (ignored: Exception) {
                    logger.error("Failed to delete file[${it.sha256}] on [$storageCredentials].", ignored)
                    failedCount += 1
                }
                totalCount += 1
            }
        }
        val elapseTimeMillis = System.currentTimeMillis() - startTimeMillis
        logger.info(
            "Clean up [$totalCount] files with zero reference, success[$cleanupCount], failed[$failedCount], " +
                "file missing[$fileMissingCount], elapse [$elapseTimeMillis] ms totally."
        )
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
