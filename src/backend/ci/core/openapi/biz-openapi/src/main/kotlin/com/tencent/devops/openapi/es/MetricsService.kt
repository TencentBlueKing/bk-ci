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
package com.tencent.devops.openapi.es

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.openapi.dao.MetricsForApiDao
import com.tencent.devops.openapi.dao.MetricsForProjectDao
import com.tencent.devops.openapi.es.impl.ESServiceImpl
import com.tencent.devops.openapi.pojo.MetricsApiData
import com.tencent.devops.openapi.pojo.MetricsProjectData
import java.time.Duration
import java.time.LocalTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class MetricsService(
    private val dslContext: DSLContext,
    private val apiDao: MetricsForApiDao,
    private val projectDao: MetricsForProjectDao,
    private val esServiceImpl: ESServiceImpl,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MetricsService::class.java)
        private const val ES_INDEX_CLOSE_JOB_KEY = "openapi:es:index:close:job:lock:key"
        private const val EACH_HOUR = 12
        private const val MOD_HOUR = 4
        private const val EACH_DAY = 288
        private const val MOD_DAY = 10
    }

    /**
     * every 5m
     */
//    @Scheduled(cron = "0 0/5 * * * ?")
    fun job() {
        logger.info("Start to openapi metrics job")
        RedisLock(redisOperation, ES_INDEX_CLOSE_JOB_KEY, 60).run {
            if (!this.tryLock()) {
                return
            }
            val begin = System.currentTimeMillis()
            val keyMap = mutableMapOf<String, MetricsApiData>()
            apiDao.batchGet(dslContext).associateByTo(keyMap) { "${it.api}@${it.key}" }
            val between = Duration.between(LocalTime.of(0, 0), LocalTime.now()).toMinutes().toInt() / 5
            val hourMod = between % EACH_HOUR
            val dayMod = between % EACH_DAY
            logger.info("openapi metrics job dayMod=$dayMod")
            esServiceImpl.executeElasticsearchQueryS(keyMap = keyMap, newDay = dayMod == MOD_DAY)
            // 每小时
            if (hourMod == MOD_HOUR) {
                logger.info("Start to openapi metrics job for 1H")
                jobFor1H(keyMap)
            }
            // 每天
            if (dayMod == MOD_DAY) {
                logger.info("Start to openapi metrics job for 1D")
                jobFor24H(keyMap)
                jobFor7D(keyMap)
            }
            apiDao.createOrUpdate(
                dslContext = dslContext,
                metricsApis = keyMap.values.toList(),
                perHour = hourMod == MOD_HOUR,
                perDay = dayMod == MOD_DAY
            )

            logger.info("execution time ${System.currentTimeMillis() - begin} millisecond")
        }
    }

    private fun jobFor1H(keyMap: MutableMap<String, MetricsApiData>) {
        esServiceImpl.executeElasticsearchQueryM(keyMap, "1h") { count, data ->
            data.call1h = count
            return@executeElasticsearchQueryM data
        }
        val projects = mutableListOf<MetricsProjectData>()
        esServiceImpl.executeElasticsearchQueryP(projects)
        projectDao.createOrUpdate(dslContext, projects)
    }

    private fun jobFor24H(keyMap: MutableMap<String, MetricsApiData>) {
        esServiceImpl.executeElasticsearchQueryM(keyMap, "1d") { count, data ->
            data.call24h = count
            return@executeElasticsearchQueryM data
        }
    }

    private fun jobFor7D(keyMap: MutableMap<String, MetricsApiData>) {
        esServiceImpl.executeElasticsearchQueryM(keyMap, "7d") { count, data ->
            data.call7d = count
            return@executeElasticsearchQueryM data
        }
    }
}
