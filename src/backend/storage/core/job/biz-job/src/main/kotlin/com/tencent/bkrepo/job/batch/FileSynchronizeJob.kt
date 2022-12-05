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

import com.tencent.bkrepo.common.api.util.executeAndMeasureTime
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.job.batch.base.BatchJob
import com.tencent.bkrepo.job.batch.base.JobContext
import com.tencent.bkrepo.job.config.FileSynchronizeJobProperties
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

/**
 * 当文件上传存储失败时，后台任务进行补偿上传
 * */
class FileSynchronizeJob(
    private val properties: FileSynchronizeJobProperties,
    private val storageCredentialsClient: StorageCredentialsClient,
    private val storageService: StorageService
) : BatchJob(properties) {

    @Scheduled(cron = "0 0 */1 * * ?") // 每小时同步一次文件
    override fun start(): Boolean {
        return super.start()
    }

    override fun doStart(jobContext: JobContext) {
        // cleanup default storage
        syncStorage()
        // cleanup extended storage
        storageCredentialsClient.list(properties.region)
            .data?.forEach { syncStorage(it) }
    }

    private fun syncStorage(storage: StorageCredentials? = null) {
        val key = storage?.key ?: "default"
        logger.info("Starting to synchronize file on storage[$key]")
        executeAndMeasureTime {
            storageService.synchronizeFile(storage)
        }.apply {
            logger.info("Synchronize file on storage[$key] completed.")
            logger.info(
                "Walked [${first.totalCount}] files totally, " +
                    "synchronized[${first.synchronizedCount}]," +
                    "${first.cleanupFolder}/${first.totalFolder} dirs deleted," +
                    "error[${first.errorCount}], ignored[${first.ignoredCount}], " +
                    "[${first.totalSize}] bytes totally, elapse [${second.seconds}] s."
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileSynchronizeJob::class.java)
    }
}
