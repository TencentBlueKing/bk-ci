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

import com.tencent.devops.common.es.client.LogClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.configuration.StorageProperties
import com.tencent.devops.log.cron.IndexCleanJob
import com.tencent.devops.log.util.IndexNameUtils.LOG_INDEX_PREFIX
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Suppress("MagicNumber")
@Component
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
class IndexCleanJobESImpl @Autowired constructor(
    storageProperties: StorageProperties,
    private val client: LogClient,
    private val redisOperation: RedisOperation
) : IndexCleanJob {

    private var coldIndexInDay = storageProperties.coldInDay ?: Int.MAX_VALUE
    private var deleteIndexInDay = storageProperties.deleteInDay ?: Int.MAX_VALUE
    private var makeIndexColdKey = storageProperties.makeColdKey ?: "routing.allocation.include.tag"
    private var makeIndexColdValue = storageProperties.makeColdValue ?: "cold"

    /**
     * 2 am every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    override fun cleanIndex() {
        logger.info("Start to clean index")
        // #9602 每个实例轮流获得锁进行幂等清理操作
        RedisLock(redisOperation, ES_INDEX_CLOSE_JOB_KEY, 20).use {
            try {
                makeColdESIndexes()
                deleteESIndexes()
            } catch (ignore: Throwable) {
                logger.warn("Fail to clean the index", ignore)
            }
        }
    }

    private fun makeColdESIndexes() {
        client.getActiveClients().forEach { c ->
            val response = c.restClient
                .indices()
                .get(GetIndexRequest("$LOG_INDEX_PREFIX*"), RequestOptions.DEFAULT)
            val indexNames = response.indices
            logger.info("Get all indices in es[${c.clusterName}] line: $indexNames")
            if (indexNames.isEmpty()) {
                return
            }

            val deathLine = LocalDateTime.now()
                .minus(coldIndexInDay.toLong(), ChronoUnit.DAYS)
            logger.info("Get the death line - ($deathLine)")
            indexNames.forEach { index ->
                if (expire(deathLine, index)) {
                    makeColdESIndex(c.restClient, index)
                }
            }
        }
    }

    private fun makeColdESIndex(c: RestHighLevelClient, index: String) {
        val request = UpdateSettingsRequest(index).settings(
            Settings.builder()
                .put(makeIndexColdKey, makeIndexColdValue)
        )
        logger.info("[$index][$makeIndexColdKey][$makeIndexColdValue] Make cold request: $request")
        val resp = c.indices().putSettings(request, RequestOptions.DEFAULT)
        logger.info("Get the config es response - ${resp.isAcknowledged}")
    }

    private fun deleteESIndexes() {
        client.getActiveClients().forEach { c ->
            val response = c.restClient
                .indices()
                .get(GetIndexRequest("$LOG_INDEX_PREFIX*"), RequestOptions.DEFAULT)

            if (response.indices.isEmpty()) {
                return
            }

            val deathLine = LocalDateTime.now()
                .minus(deleteIndexInDay.toLong(), ChronoUnit.DAYS)
            logger.info("Get the death line - ($deathLine)")
            response.indices.forEach { index ->
                if (expire(deathLine, index)) {
                    deleteESIndex(c.restClient, index)
                }
            }
        }
    }

    private fun deleteESIndex(c: RestHighLevelClient, index: String) {
        logger.info("[$index] Start to delete ES index")
        val resp = c.indices()
            .delete(DeleteIndexRequest(index), RequestOptions.DEFAULT)
        logger.info("Get the delete es response - ${resp.isAcknowledged}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexCleanJobESImpl::class.java)
        private const val ES_INDEX_CLOSE_JOB_KEY = "log:es:index:close:job:lock:key"
    }
}
