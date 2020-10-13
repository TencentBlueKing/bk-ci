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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.cron

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.util.IndexNameUtils.LOG_PREFIX
import org.elasticsearch.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Component
class ESIndexCloseJob @Autowired constructor(
    private val client: LogClient,
    private val redisOperation: RedisOperation
) {

    private var closeIndexInDay = 30 // default is expire in 30 days
    private var deleteIndexInDay = 90 // default be deleted in 90 days

    /**
     * 2 am every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun closeIndex() {
        logger.info("Start to close index")
        val redisLock = RedisLock(redisOperation, ES_INDEX_CLOSE_JOB_KEY, 20)
        try {
            if (!redisLock.tryLock()) {
                logger.info("The other process is processing clean job, ignore")
                return
            }
            closeESIndexes()
            deleteESIndexes()
        } catch (t: Throwable) {
            logger.warn("Fail to close the index", t)
        } finally {
            redisLock.unlock()
        }
    }

    fun updateExpireIndexDay(expired: Int) {
        logger.warn("Update the expire index day from $expired to ${this.closeIndexInDay}")
        if (expired <= 10) {
            logger.warn("The expired is illegal")
            throw OperationException("Expired is illegal")
        }
        this.closeIndexInDay = expired
    }

    fun getExpireIndexDay() = closeIndexInDay

    private fun closeESIndexes() {
        client.getActiveClients().forEach { c ->
            val indexes = c.client.admin()
                .indices()
                .prepareGetIndex()
                .get()

            if (indexes.indices.isEmpty()) {
                return
            }

            val deathLine = LocalDateTime.now()
                .minus(closeIndexInDay.toLong(), ChronoUnit.DAYS)
            logger.info("Get the death line - ($deathLine)")
            indexes.indices.forEach { index ->
                if (expire(deathLine, index)) {
                    closeESIndex(c.client, index)
                }
            }
        }
    }

    private fun closeESIndex(c: Client, index: String) {
        logger.info("[$index] Start to close ES index")
        val resp = c.admin()
            .indices()
            .prepareClose(index)
            .get()
        logger.info("Get the close es response - ${resp.isAcknowledged}")
    }

    private fun deleteESIndexes() {
        client.getActiveClients().forEach { c ->
            val indexes = c.client.admin()
                .indices()
                .prepareGetIndex()
                .get()

            if (indexes.indices.isEmpty()) {
                return
            }

            val deathLine = LocalDateTime.now()
                .minus(deleteIndexInDay.toLong(), ChronoUnit.DAYS)
            logger.info("Get the death line - ($deathLine)")
            indexes.indices.forEach { index ->
                if (expire(deathLine, index)) {
                    deleteESIndex(c.client, index)
                }
            }
        }
    }

    private fun deleteESIndex(c: Client, index: String) {
        logger.info("[$index] Start to delete ES index")
        val resp = c.admin()
            .indices()
            .prepareDelete(index)
            .get()
        logger.info("Get the delete es response - ${resp.isAcknowledged}")
    }

    private fun expire(deathLine: LocalDateTime, index: String): Boolean {
        try {
            if (!index.startsWith(LOG_PREFIX)) {
                return false
            }
            val dateStr = index.replace(LOG_PREFIX, "") + " 00:00"
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = LocalDateTime.parse(dateStr, format)

            if (deathLine > date) {
                logger.info("[$index] The index is expire ($deathLine|$date)")
                return true
            }
        } catch (t: Throwable) {
            logger.warn("[$index] Fail to check if the index expire", t)
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ESIndexCloseJob::class.java)
        private const val ES_INDEX_CLOSE_JOB_KEY = "log:es:index:close:job:lock:key"
    }
}