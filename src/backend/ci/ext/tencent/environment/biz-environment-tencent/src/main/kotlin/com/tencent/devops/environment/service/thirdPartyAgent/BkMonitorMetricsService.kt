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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.BkMonitorRequestBody
import com.tencent.devops.environment.pojo.BkMonitorRequestBodyQueryConfigs
import com.tencent.devops.environment.pojo.BkMonitorResp
import com.tencent.devops.environment.pojo.BkMonitorRespDataSeries
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
import javax.ws.rs.NotFoundException

@Service
class BkMonitorMetricsService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao
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

        //        const val TIME_RANGE_HOUR = "HOUR"
        const val TIME_RANGE_DAY = "DAY"
        const val TIME_RANGE_WEEK = "WEEK"
    }

    fun queryMemoryUsageMetrics(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val promql = "avg($dataTableName:mem:pct_used{agentId=\"$nodeHashId\",projectId=\"$projectId\"})"

        val data = searchMetrics(promql, timeRange)?.firstOrNull()?.datapoints

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
            logger.warn("$userId|$projectId|$nodeHashId|$timeRange mem metrics is empty")
            listOf()
        } else {
            res
        }

        return resultData
    }

    fun queryCpuUsageMetrics(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val promql = "avg($dataTableName:cpu_detail:user{agentId=\"$nodeHashId\",projectId=\"$projectId\"})"

        val data = searchMetrics(promql, timeRange)?.firstOrNull()?.datapoints

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
            logger.warn("$userId|$projectId|$nodeHashId|$timeRange cpu metrics is empty")
            listOf()
        } else {
            res
        }

        return resultData
    }

    fun queryDiskioMetrics(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        val groupByTime: String = when (timeRange) {
            TIME_RANGE_WEEK -> "10m"
            TIME_RANGE_DAY -> "2m"
            else -> "10s"
        }
        val (readPromql, writePromql) = when (OS.valueOf(agentRecord.os)) {
            OS.MACOS, OS.LINUX -> Pair(
                "abs(avg(rate($dataTableName:io:rkb_s{agentId=\"$nodeHashId\"," +
                    "projectId=\"$projectId\"}[$groupByTime])) by (name))",
                "abs(avg(rate($dataTableName:io:wkb_s{agentId=\"$nodeHashId\"," +
                    "projectId=\"$projectId\"}[$groupByTime])) by (name))"
            )

            OS.WINDOWS -> Pair(
                "avg($dataTableName:io:rkb_s{agentId=\"$nodeHashId\",projectId=\"$projectId\"}) by (name)",
                "avg($dataTableName:io:wkb_s{agentId=\"$nodeHashId\",projectId=\"$projectId\"}) by (name)"
            )

            else -> return emptyMap()
        }

        val readData = searchMetrics(readPromql, timeRange)
        val writeData = searchMetrics(writePromql, timeRange)

        val result = mutableMapOf<String, List<Map<String, Any>>>()
        result.putAll(formatData("name", "read", readData))
        result.putAll(formatData("name", "write", writeData))
        return result
    }

    fun queryNetMetrics(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        val groupByTime: String = when (timeRange) {
            TIME_RANGE_WEEK -> "10m"
            TIME_RANGE_DAY -> "2m"
            else -> "10s"
        }
        val (readPromql, sendPromql) = when (OS.valueOf(agentRecord.os)) {
            OS.MACOS, OS.LINUX -> Pair(
                "abs(avg(rate($dataTableName:net:speed_recv{agentId=\"$nodeHashId\"," +
                    "projectId=\"$projectId\"}[$groupByTime])) by (name))",
                "abs(avg(rate($dataTableName:net:speed_sent{agentId=\"$nodeHashId\"," +
                    "projectId=\"$projectId\"}[$groupByTime])) by (name))"
            )

            OS.WINDOWS -> Pair(
                "avg($dataTableName:net:speed_recv{agentId=\"$nodeHashId\",projectId=\"$projectId\"}) by (name)",
                "avg($dataTableName:net:speed_sent{agentId=\"$nodeHashId\",projectId=\"$projectId\"}) by (name)"
            )

            else -> return emptyMap()
        }

        val readData = searchMetrics(readPromql, timeRange)
        val sendData = searchMetrics(sendPromql, timeRange)

        val result = mutableMapOf<String, List<Map<String, Any>>>()
        result.putAll(formatData("interface", "IN", readData))
        result.putAll(formatData("interface", "OUT", sendData))
        return result
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
        return mapOf(
            label to (item.getOrNull(0) ?: 0),
            "time" to if (item.getOrNull(1) == null) {
                ""
            } else {
                DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(item[1].toLong()))
            }
        )
    }

    private fun searchMetrics(promql: String, timeRange: String): List<BkMonitorRespDataSeries>? {
        val body = BkMonitorRequestBody(
            // TODO: 未来看如何获取
            bkBizId = -4220817,
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
            endTime = Instant.now().epochSecond,
            slimit = 500,
            downSampleRange = "2s"
        )

        return requestBkMonitor(body).data?.series
    }

    private fun requestBkMonitor(body: Any): BkMonitorResp {
        val url = "$bkMonitorGateway/time_series/unify_query"
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to bkMonitorAppCode, "bk_app_secret" to bkMonitorAppSecret)
        ).replace("\\s".toRegex(), "")
        val requestBody = objectMapper.writeValueAsString(body)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-bkapi-authorization", headerStr)
            .build()

        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val resp = objectMapper.readValue<BkMonitorResp>(responseStr)
            if (resp.code != 0L || !resp.result) {
                // 请求错误
                logger.warn("request failed, url:($url)|response:($it)")
                throw RemoteServiceException("request failed, response:(${resp.message})")
            }
            logger.debug("request response：${objectMapper.writeValueAsString(resp.data)}")
            return resp
        }
    }
}
