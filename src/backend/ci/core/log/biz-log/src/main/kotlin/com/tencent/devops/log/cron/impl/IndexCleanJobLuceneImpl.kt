/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.log.cron.impl

import com.tencent.devops.log.configuration.StorageProperties
import com.tencent.devops.log.cron.IndexCleanJob
import com.tencent.devops.log.lucene.LuceneClient
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Lucene 索引清理。只在 log.storage.type=lucene 时注册，不参与 Elasticsearch 的冷热和删除。
 * 冷数据搬到 dataDirectory/cold，到期后热目录和冷目录一起删。
 */
@Suppress("MagicNumber")
@Component
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "lucene")
class IndexCleanJobLuceneImpl @Autowired constructor(
    storageProperties: StorageProperties,
    private val luceneClient: LuceneClient
) : IndexCleanJob {

    private var coldIndexInDay = storageProperties.coldInDay ?: Int.MAX_VALUE
    private var deleteIndexInDay = storageProperties.deleteInDay ?: Int.MAX_VALUE

    /**
     * 2 am every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    override fun cleanIndex() {
        logger.info("Start to clean lucene index")
        val ran = luceneClient.withCleanLock {
            runCatching { archiveLuceneIndexes() }
                .onFailure { logger.warn("Fail to archive lucene indexes", it) }
            runCatching { deleteLuceneIndexes() }
                .onFailure { logger.warn("Fail to delete lucene indexes", it) }
            true
        }
        if (ran == null) {
            logger.info("The other process is processing lucene clean job, ignore")
        }
    }

    private fun archiveLuceneIndexes() {
        val deathLine = deathLine(coldIndexInDay) ?: run {
            logger.info("lucene index archive is disabled")
            return
        }
        val deleteLine = deathLine(deleteIndexInDay)
        logger.info("Get the lucene cold death line - ($deathLine)")
        luceneClient.listIndices(cold = false).forEach { index ->
            if (!expire(deathLine, index)) {
                return@forEach
            }
            if (deleteLine != null && expire(deleteLine, index)) {
                return@forEach
            }
            try {
                if (luceneClient.archiveIndex(index)) {
                    logger.info("[$index] archived lucene index to cold directory")
                }
            } catch (ignore: Throwable) {
                logger.warn("[$index] Fail to archive lucene index, skip", ignore)
            }
        }
    }

    private fun deleteLuceneIndexes() {
        val deathLine = deathLine(deleteIndexInDay) ?: run {
            logger.info("lucene index delete is disabled")
            return
        }
        logger.info("Get the lucene delete death line - ($deathLine)")
        val indexes = (luceneClient.listIndices(cold = false) + luceneClient.listIndices(cold = true)).distinct()
        indexes.forEach { index ->
            if (!expire(deathLine, index)) {
                return@forEach
            }
            try {
                if (luceneClient.deleteIndex(index)) {
                    logger.info("[$index] deleted lucene index")
                }
            } catch (ignore: Throwable) {
                logger.warn("[$index] Fail to delete lucene index, skip", ignore)
            }
        }
    }

    private fun deathLine(days: Int): LocalDateTime? {
        if (days <= 0 || days == Int.MAX_VALUE) {
            return null
        }
        return LocalDateTime.now().minus(days.toLong(), ChronoUnit.DAYS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexCleanJobLuceneImpl::class.java)
    }
}
