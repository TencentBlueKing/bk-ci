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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_diskio_read_bytes
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_diskio_write_bytes
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.t_diskio
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import org.influxdb.dto.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@AgentMetrics(metricsType = UsageMetrics.MetricsType.DISK, osTypes = [OS.LINUX, OS.MACOS])
class LinuxDiskioMetrics @Autowired constructor(val influxdbClient: InfluxdbClient) : UsageMetrics {

    private val emptyDiskioMetrics = mapOf("dev" to listOf(mapOf("time" to "0")))

    @PostConstruct
    private fun init() {
        UsageMetrics.registerBean(this)
    }

    override fun loadQuery(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timeGroupBy = getTimeGroupBy(timeRange)
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT non_negative_derivative(mean(\"$f_diskio_read_bytes\"), $timeGroupBy) as \"read\" " +
                    "FROM \"$t_diskio\" " +
                    "WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart, \"name\" fill(null); " +
                    "SELECT non_negative_derivative(mean(\"$f_diskio_write_bytes\"), $timeGroupBy)  as \"write\" " +
                    "FROM \"$t_diskio\" " +
                    "WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart, \"name\" fill(null)"

        val queryResult = try {
            influxdbClient.getInfluxDb()?.query(Query(queryStr, UsageMetrics.DB)) ?: return emptyDiskioMetrics
        } catch (ignore: Exception) {
            return emptyDiskioMetrics
        }

        if (queryResult.hasError()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_INFLUX_QUERY_DISK_INFO_FAIL,
                params = arrayOf(queryResult.error)
            )
        }

        return buildSerialDataByTag(queryResult, "name")
    }
}
