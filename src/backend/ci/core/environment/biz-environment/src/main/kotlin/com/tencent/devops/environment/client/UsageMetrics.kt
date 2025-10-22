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

package com.tencent.devops.environment.client

import com.tencent.devops.common.api.pojo.OS
import org.influxdb.dto.QueryResult

interface UsageMetrics {

    /**
     *  查询[agentHashId]agent的指定时间范围[timeRange]的使用指标数据
     */
    fun loadQuery(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>>

    fun getTimePart(timeRange: String): String {
        return when (timeRange) {
            TIME_RANGE_HOUR -> TIME_PART_HOUR
            TIME_RANGE_WEEK -> TIME_PART_WEEK
            else -> TIME_PART_DAY
        }
    }

    fun getTimeGroupBy(timeRange: String): String {
        return when (timeRange) {
            TIME_RANGE_HOUR -> TIME_GROUP_BY_HOUR
            TIME_RANGE_WEEK -> TIME_GROUP_BY_WEEK
            else -> TIME_GROUP_BY_DAY
        }
    }

    fun loadSerialData(result: QueryResult.Result, serialName: String): List<Map<String, Any>> {
        if (result.hasError()) {
            return listOf()
        }
        return if (result.series == null || result.series.isEmpty()) {
            listOf()
        } else {
            result.series[0].values.map {
                mapOf("time" to it[0], serialName to it[1])
            }
        }
    }

    fun buildSerialDataByTag(queryResult: QueryResult, tag: String): Map<String, List<Map<String, Any>>> {
        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        queryResult.results.forEach { qResult ->
            qResult.series?.forEach { qSerial ->
                val serialName = "${qSerial.tags[tag]}:${qSerial.columns[1]}"
                resultData[serialName] = qSerial.values.map {
                    mapOf("time" to it[0], serialName to it[1])
                }
            }
        }
        return resultData
    }

    companion object {
        val emptyMetrics = listOf(mapOf("time" to "0"))
        const val DB = "agentMetrix"
        private const val TIME_RANGE_HOUR = "HOUR"

        // private const val TIME_RANGE_DAY = "DAY"
        private const val TIME_RANGE_WEEK = "WEEK"
        private const val TIME_PART_HOUR = "time >= now() - 1h and time <= now() - 30s GROUP BY time(1m)"
        private const val TIME_PART_DAY = "time >= now() - 24h and time <= now() - 30s GROUP BY time(1m)"
        private const val TIME_PART_WEEK = "time >= now() - 7d and time <= now() - 30s GROUP BY time(10m)"
        private const val TIME_GROUP_BY_HOUR = "10s"
        private const val TIME_GROUP_BY_DAY = "2m"
        private const val TIME_GROUP_BY_WEEK = "10m"

        private val map = mutableMapOf<String, UsageMetrics>()

        internal fun registerBean(bean: UsageMetrics) {
            val agentMetrics = bean.javaClass.getAnnotation(AgentMetrics::class.java)
            agentMetrics.osTypes.forEach { os ->
                map[genKey(agentMetrics.metricsType, os)] = bean
            }
        }

        fun loadMetricsBean(metricsType: MetricsType, os: OS): UsageMetrics? {
            return map[genKey(metricsType, os)]
        }

        private fun genKey(metricsType: MetricsType, os: OS) = "${metricsType.name}:${os.name}"
    }

    enum class MetricsType {
        CPU, MEMORY, DISK, NET
    }
}
