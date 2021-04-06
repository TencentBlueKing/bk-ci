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

import com.tencent.bkrepo.common.api.util.executeAndMeasureTime
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.service.StorageCredentialService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 清理缓存文件定时任务
 */
@Component
class ExpiredCacheFileCleanupJob(
    private val storageService: StorageService,
    private val storageCredentialService: StorageCredentialService
) {

    @Scheduled(cron = "0 0 4 * * ?") // 每天凌晨4点执行
    @SchedulerLock(name = "ExpiredCacheFileCleanupJob", lockAtMostFor = "P7D")
    fun cleanup() {
        logger.info("Starting to clean up temp and expired cache files.")
        // cleanup default storage
        cleanUpOnStorage()
        // cleanup extended storage
        storageCredentialService.list().forEach {
            cleanUpOnStorage(it)
        }
        logger.info("Clean up completed.")
    }

    private fun cleanUpOnStorage(storage: StorageCredentials? = null) {
        val key = storage?.key ?: "default"
        logger.info("Starting to clean up on storage [$key].")
        executeAndMeasureTime {
            storageService.cleanUp(storage)
        }.apply {
            logger.info("Clean up on storage[$key] completed, elapse [${second.seconds}] s.")
        }
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
