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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("MagicNumber")
@Component
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "lucene")
class IndexCleanJobLuceneImpl @Autowired constructor(
    storageProperties: StorageProperties,
    private val luceneClient: LuceneClient
) : IndexCleanJob {

    private var deleteIndexInDay = storageProperties.deleteInDay ?: Int.MAX_VALUE

    /**
     * 2 am every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    override fun cleanIndex() {
        logger.info("Start to clean index")
        try {
            deleteLuceneIndexes()
        } catch (ignore: Throwable) {
            logger.warn("Fail to clean the index", ignore)
        }
    }

    private fun deleteLuceneIndexes() {
        val indexes = luceneClient.listIndices()
        if (indexes.isEmpty()) {
            return
        }
        val deathLine = LocalDateTime.now()
            .minus(deleteIndexInDay.toLong(), ChronoUnit.DAYS)
        logger.info("Get the death line - ($deathLine)")
        indexes.forEach { index ->
            if (expire(deathLine, index)) {
                luceneClient.deleteIndex(index)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexCleanJobLuceneImpl::class.java)
    }
}
