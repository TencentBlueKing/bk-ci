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

package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.BkBizProjectDao
import com.tencent.devops.environment.model.AgentHostInfo
import com.tencent.devops.environment.pojo.BkBizProjectLock
import com.tencent.devops.environment.pojo.BkMetadataResp
import com.tencent.devops.environment.pojo.BkMonitorRequestBody
import com.tencent.devops.environment.pojo.BkMonitorRequestBodyQueryConfigs
import com.tencent.devops.environment.pojo.BkMonitorResp
import com.tencent.devops.environment.pojo.BkMonitorRespData
import com.tencent.devops.environment.pojo.BkMonitorRespDataSeries
import com.tencent.devops.environment.utils.NumberUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class BkMonitorMetricsService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val bkBizProjectDao: BkBizProjectDao,
    private val redisOperation: RedisOperation
) {

    @Value("\${bkMonitor.gateway:#{null}}")
    val bkMonitorGateway: String? = null

    @Value("\${bkMonitor.appCode:#{null}}")
    val bkMonitorAppCode: String? = null

    @Value("\${bkMonitor.appSecret:#{null}}")
    val bkMonitorAppSecret: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(AgentMetricService::class.java)
        private const val dataTableName = "agentmetric"

        const val TIME_RANGE_HOUR = "HOUR"
        const val TIME_RANGE_DAY = "DAY"
        const val TIME_RANGE_WEEK = "WEEK"

        private const val MAX_CACHE = 1000L
        private val emptyInfo = AgentHostInfo(nCpus = "0", memTotal = "0", diskTotal = "0")
    }

    fun queryMemoryUsageMetrics(
        userId: String,
        projectId: String,
        agentHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val promql = "avg($dataTableName:mem:pct_used{agentId=\"$agentHashId\",projectId=\"$projectId\"})"

        val data = searchMetrics(projectId, promql, timeRange)?.firstOrNull()?.datapoints

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        val res = data?.map { d ->
            mapOf(
                "used_percent" to (d.getOrNull(0) ?: 0),
                "time" to if (d.getOrNull(1) == null) {
                    ""
                } else {
                    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(d[1].toLong()))
                }
            )
        }
        resultData["used_percent"] = if (res.isNullOrEmpty()) {
            logger.warn("$userId|$projectId|$agentHashId|$timeRange mem metrics is empty")
            listOf()
        } else {
            res
        }

        return resultData
    }

    fun queryCpuUsageMetrics(
        userId: String,
        projectId: String,
        agentHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val promql = "avg($dataTableName:cpu_detail:user{agentId=\"$agentHashId\",projectId=\"$projectId\"})"

        val data = searchMetrics(projectId, promql, timeRange)?.firstOrNull()?.datapoints

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        val res = data?.map { d ->
            mapOf(
                "usage_user" to (d.getOrNull(0) ?: 0),
                "time" to if (d.getOrNull(1) == null) {
                    ""
                } else {
                    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(d[1].toLong()))
                }
            )
        }
        resultData["usage_user"] = if (res.isNullOrEmpty()) {
            logger.warn("$userId|$projectId|$agentHashId|$timeRange cpu metrics is empty")
            listOf()
        } else {
            res
        }

        return resultData
    }

    fun queryDiskioMetrics(
        userId: String,
        projectId: String,
        agentHashId: String,
        os: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val groupByTime: String = when (timeRange) {
            TIME_RANGE_WEEK -> "10m"
            TIME_RANGE_DAY -> "2m"
            else -> "10s"
        }
        val tag = when (OS.valueOf(os)) {
            OS.MACOS, OS.LINUX -> "name"
            OS.WINDOWS -> "instance"
        }
//        val (readPromql, writePromql) = when (OS.valueOf(agentRecord.os)) {
//            OS.MACOS, OS.LINUX -> Pair(
//                "abs(avg(rate($dataTableName:io:rkb_s{agentId=\"$agentId\"," +
//                    "projectId=\"$projectId\"}[$groupByTime])) by ($tag))",
//                "abs(avg(rate($dataTableName:io:wkb_s{agentId=\"$agentId\"," +
//                    "projectId=\"$projectId\"}[$groupByTime])) by ($tag))"
//            )
//
//            OS.WINDOWS -> Pair(
//                "avg($dataTableName:io:rkb_s{agentId=\"$agentId\",projectId=\"$projectId\"}) by ($tag)",
//                "avg($dataTableName:io:wkb_s{agentId=\"$agentId\",projectId=\"$projectId\"}) by ($tag)"
//            )
//
//            else -> return emptyMap()
//        }
        val readPromql = "abs(avg(rate($dataTableName:io:rkb_s{agentId=\"$agentHashId\"," +
                "projectId=\"$projectId\"}[$groupByTime])) by ($tag))"
        val writePromql = "abs(avg(rate($dataTableName:io:wkb_s{agentId=\"$agentHashId\"," +
                "projectId=\"$projectId\"}[$groupByTime])) by ($tag))"

        val readData = searchMetrics(projectId, readPromql, timeRange)
        val writeData = searchMetrics(projectId, writePromql, timeRange)

        val result = mutableMapOf<String, List<Map<String, Any>>>()
        result.putAll(formatData(tag, "read", readData))
        result.putAll(formatData(tag, "write", writeData))
        return result
    }

    fun queryNetMetrics(
        userId: String,
        projectId: String,
        agentHashId: String,
        os: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val groupByTime: String = when (timeRange) {
            TIME_RANGE_WEEK -> "10m"
            TIME_RANGE_DAY -> "2m"
            else -> "10s"
        }
        val tag = when (OS.valueOf(os)) {
            OS.MACOS, OS.LINUX -> "interface"
            OS.WINDOWS -> "instance"
        }
        val readPromql = "abs(avg(rate($dataTableName:net:speed_recv{agentId=\"$agentHashId\"," +
                "projectId=\"$projectId\"}[$groupByTime])) by ($tag))"
        val sendPromql = "abs(avg(rate($dataTableName:net:speed_sent{agentId=\"$agentHashId\"," +
                "projectId=\"$projectId\"}[$groupByTime])) by ($tag))"

        val readData = searchMetrics(projectId, readPromql, timeRange)
        val sendData = searchMetrics(projectId, sendPromql, timeRange)

        val result = mutableMapOf<String, List<Map<String, Any>>>()
        result.putAll(formatData(tag, "IN", readData))
        result.putAll(formatData(tag, "OUT", sendData))
        return result
    }

    fun queryHostInfo(agentAndProjectId: String): AgentHostInfo {
        return hostCache.get(agentAndProjectId) ?: emptyInfo
    }

    // #7479 主机的信息变化不会经常变化，除非更换机器，所以不需要经常进行直接查询，更新缓存即可。
    private val hostCache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, AgentHostInfo> { agentAndProjectId ->
            val (projectId, agentId) = agentAndProjectId.split(":")
            queryHostInfoImpl(projectId, agentId)
        }

    private fun queryHostInfoImpl(projectId: String, agentHashId: String): AgentHostInfo {
        val nCpuPromql = "$dataTableName:load:n_cpus{agentId=\"$agentHashId\"}"
        val nCpu = searchMetrics(
            projectId = projectId,
            promql = nCpuPromql,
            timeRange = TIME_RANGE_HOUR
        )?.get(0)?.datapoints?.lastOrNull { it[0] != 0.0 }?.get(0)

        val memPromql = "$dataTableName:mem:total{agentId=\"$agentHashId\"}"
        val nMem = searchMetrics(
            projectId = projectId,
            promql = memPromql,
            timeRange = TIME_RANGE_HOUR
        )?.get(0)?.datapoints?.lastOrNull { it[0] != 0.0 }?.get(0)
        val memTotal = if (nMem != null) {
            NumberUtils.byteToString(nMem)
        } else {
            "0"
        }

        return AgentHostInfo(
            nCpus = nCpu?.toInt()?.toString() ?: "0",
            memTotal = memTotal,
            diskTotal = "0"
        )
    }

    private fun formatData(
        tag: String,
        label: String,
        series: List<BkMonitorRespDataSeries>?
    ): Map<String, List<Map<String, Any>>> {
        if (series.isNullOrEmpty()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, List<Map<String, Any>>>()
        series.forEach { s ->
            val dimension = s.dimensions?.get(tag) ?: return@forEach
            val fullLabel = "$dimension:$label"
            val data = s.datapoints?.map {
                parseDatapointItem(fullLabel, it)
            } ?: listOf()
            result[fullLabel] = data
        }

        return result
    }

    private fun parseDatapointItem(label: String, item: List<Double>): Map<String, Any> {
        // 前端计算需要数据*10与开源版influxdb相同
        val data = if (item.getOrNull(0) == null) {
            0
        } else {
            item[0] * 10
        }
        return mapOf(
            label to data,
            "time" to if (item.getOrNull(1) == null) {
                ""
            } else {
                DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(item[1].toLong()))
            }
        )
    }

    private fun searchMetrics(projectId: String, promql: String, timeRange: String): List<BkMonitorRespDataSeries>? {
        val startTime = System.currentTimeMillis()
        val bizId = getBizId(projectId) ?: return null
        val body = BkMonitorRequestBody(
            bkBizId = bizId.inv() + 1,
            queryConfigs = listOf(
                BkMonitorRequestBodyQueryConfigs(
                    alias = "a",
                    dataSourceLabel = "prometheus",
                    dataTypeLabel = "time_series",
                    promql = promql,
                    interval = when (timeRange) {
                        TIME_RANGE_WEEK -> 600
                        else -> 60
                    }
                )
            ),
            expression = "",
            startTime = when (timeRange) {
                TIME_RANGE_WEEK -> Instant.now().minusSeconds(604800).epochSecond
                TIME_RANGE_DAY -> Instant.now().minusSeconds(86400).epochSecond
                else -> Instant.now().minusSeconds(3600).epochSecond
            },
            endTime = Instant.now().minusSeconds(30).epochSecond,
            slimit = 500,
            downSampleRange = "2s"
        )

        val data = requestBkMonitor(body)?.series

        logger.info("searchMetrics $promql cost ${System.currentTimeMillis() - startTime}ms")

        return data
    }

    private fun getBizId(projectId: String): Long? {
        var bizId = bkBizProjectDao.fetchBizId(dslContext, projectId)
        if (bizId != null) {
            return bizId
        }
        // 因为同时会有四个方法进行查询，所以加锁写入
        val bizLock = BkBizProjectLock(
            redisOperation = redisOperation,
            projectId = projectId
        )
        try {
            bizLock.lock()
            // 进来后再查询下，因为会有其他同时强锁的写入
            bizId = bkBizProjectDao.fetchBizId(dslContext, projectId)
            if (bizId != null) {
                return bizId
            }
            // 没有就拿取新的
            val url = "$bkMonitorGateway/metadata_get_space_detail?space_uid=bkci__$projectId"
            val headerStr = objectMapper.writeValueAsString(
                mapOf("bk_app_code" to bkMonitorAppCode, "bk_app_secret" to bkMonitorAppSecret)
            ).replace("\\s".toRegex(), "")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Bkapi-Authorization", headerStr)
                .build()

            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    logger.warn("request failed, uri:($url)|response: ($it)")
                    throw RemoteServiceException("request failed, response:($it)")
                }
                val responseStr = it.body!!.string()
                val resp = objectMapper.readValue<BkMetadataResp>(responseStr)
                if (resp.code != 200L || !resp.result) {
                    // 请求错误
                    logger.warn("request failed, url:($url)|response:($it)")
                    throw RemoteServiceException("request failed, response:(${resp.message})")
                }
                logger.debug("request response：${objectMapper.writeValueAsString(resp.data)}")
                bizId = resp.data?.id
                if (bizId == null) {
                    logger.error("request bk mate data is null")
                    return null
                }

                bkBizProjectDao.add(dslContext, bizId!!, projectId)
                return bizId
            }
        } catch (e: Exception) {
            logger.error("get bizId error", e)
            return null
        } finally {
            bizLock.unlock()
        }
    }

    private fun requestBkMonitor(body: Any): BkMonitorRespData? {
        return try {
            doRequestBkMonitor(body)
        } catch (e: Exception) {
            logger.warn("requestBkMonitor error", e)
            null
        }
    }

    private fun doRequestBkMonitor(body: Any): BkMonitorRespData? {
        val url = "$bkMonitorGateway/time_series/unify_query"
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to bkMonitorAppCode, "bk_app_secret" to bkMonitorAppSecret)
        ).replace("\\s".toRegex(), "")
        val requestBody = objectMapper.writeValueAsString(body)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Bkapi-Authorization", headerStr)
            .build()

        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val resp = objectMapper.readValue<BkMonitorResp>(responseStr)
            if (resp.code != 200L || !resp.result) {
                // 请求错误
                logger.warn("request failed, url:($url)|response:($it)")
                throw RemoteServiceException("request failed, response:(${resp.message})")
            }
            logger.debug("request response：${objectMapper.writeValueAsString(resp.data)}")
            return resp.data
        }
    }
}
