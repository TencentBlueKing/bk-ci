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

package com.tencent.devops.environment.client

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_disk_total
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_mem_total
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_system_n_cpus
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.t_disk
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.t_mem
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.t_system
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.model.AgentHostInfo
import com.tencent.devops.environment.utils.NumberUtils
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Query
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class InfluxdbClient {
    companion object {
        private val logger = LoggerFactory.getLogger(InfluxdbClient::class.java)
        private const val DB = "agentMetrix"
        private const val MAX_CACHE = 1000L
        private val emptyInfo = AgentHostInfo(nCpus = "0", memTotal = "0", diskTotal = "0")
    }

    @Value("\${influxdb.server:}")
    val influxdbServer: String = ""

    @Value("\${influxdb.userName:}")
    val influxdbUserName: String = ""

    @Value("\${influxdb.password:}")
    val influxdbPassword: String = ""

    private val influxdb by lazy {
        if (influxdbUserName.isBlank() || influxdbPassword.isBlank()) {
            InfluxDBFactory.connect(influxdbServer)
        } else {
            InfluxDBFactory.connect(influxdbServer, influxdbUserName, influxdbPassword)
        }
    }

    fun getInfluxDb(): InfluxDB? {
        return try {
            influxdb
        } catch (fail: Exception) {
            logger.error("BKSystemErrorMonitor|getInfluxDb $influxdbServer fail, msg=${fail.message}", fail)
            null // 返回空，防止页面异常。 顶多是数量展示空。 需要用户根据日志定位问题
        }
    }

    fun queryHostInfo(agentHashId: String): AgentHostInfo {
        return hostCache.get(agentHashId) ?: emptyInfo
    }

    // #7479 主机的信息变化不会经常变化，除非更换机器，所以不需要经常进行直接查询，更新缓存即可。
    private val hostCache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, AgentHostInfo> { agentHashId -> queryHostInfoImpl(agentHashId) }

    private fun queryHostInfoImpl(agentHashId: String): AgentHostInfo {
        val queryStr =
            "SELECT last(\"$f_system_n_cpus\") FROM \"$t_system\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                    "AND time >= now() - 7d and time <= now() - 30s; " +
                    "SELECT last(\"$f_mem_total\") FROM \"$t_mem\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                    "AND time >= now() - 7d and time <= now() - 30s; " +
                    "SELECT max(\"$f_disk_total\") FROM \"$t_disk\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                    "and time >= now() - 7d and time <= now() - 30s"

        val queryResult = try {
            getInfluxDb()?.query(Query(queryStr, DB)) ?: return emptyInfo
        } catch (ignore: Exception) {
            return emptyInfo
        }

        if (queryResult.hasError()) {
            logger.error("BKSystemErrorMonitor|queryHostInfo error: ${queryResult.error}")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_INFLUX_QUERY_HOST_INFO_FAIL,
                params = arrayOf(queryResult.error)
            )
        }

        var nCpus = "0"
        var memTotal = "0"
        var diskTotal = "0"

        try {
            val nCpusSerie = queryResult.results[0].series
            if (nCpusSerie != null && nCpusSerie.isNotEmpty()) {
                nCpus = nCpusSerie[0].values[0][1].toString().toDouble().toInt().toString()
            }

            val memTotalSerie = queryResult.results[1].series
            if (memTotalSerie != null && memTotalSerie.isNotEmpty()) {
                memTotal = NumberUtils.byteToString(memTotalSerie[0].values[0][1].toString().toDouble())
            }

            val diskTotalSerie = queryResult.results[2].series
            if (diskTotalSerie != null && diskTotalSerie.isNotEmpty()) {
                diskTotal = NumberUtils.byteToString(diskTotalSerie[0].values[0][1].toString().toDouble())
            }
        } catch (ignore: Exception) {
            logger.warn("$agentHashId|parse agent host info failed", ignore)
        }

        return AgentHostInfo(nCpus, memTotal, diskTotal)
    }
}
