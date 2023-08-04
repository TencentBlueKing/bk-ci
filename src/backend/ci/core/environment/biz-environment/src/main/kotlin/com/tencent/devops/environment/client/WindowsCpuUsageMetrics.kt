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
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import org.influxdb.dto.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@AgentMetrics(metricsType = UsageMetrics.MetricsType.CPU, osTypes = [OS.WINDOWS])
class WindowsCpuUsageMetrics @Autowired constructor(val influxdbClient: InfluxdbClient) : UsageMetrics {

    companion object {
        private const val usage_privileged_idx = 0
        private const val usage_user_idx = 1
        private const val usage_interrupt_idx = 2

        private const val k_usage_privileged = "usage_privileged"
        private const val k_usage_user = "usage_user"
        private const val k_usage_interrupt = "usage_interrupt"

        private val emptyCpuMetrics = mapOf(
            k_usage_privileged to UsageMetrics.emptyMetrics,
            k_usage_user to UsageMetrics.emptyMetrics,
            k_usage_interrupt to UsageMetrics.emptyMetrics
        )
    }

    @PostConstruct
    private fun init() {
        UsageMetrics.registerBean(this)
    }

    override fun loadQuery(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT mean(\"Percent_Privileged_Time\") FROM \"win_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                "AND $timePart fill(null); " +
                "SELECT mean(\"Percent_User_Time\") FROM \"win_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                "AND $timePart fill(null); " +
                "SELECT mean(\"Percent_Interrupt_Time\") FROM \"win_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                "AND $timePart fill(null)"

        val queryResult = try {
            influxdbClient.getInfluxDb()?.query(Query(queryStr, UsageMetrics.DB)) ?: return emptyCpuMetrics
        } catch (ignore: Exception) {
            return emptyCpuMetrics
        }

        if (queryResult.hasError()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_INFLUX_QUERY_CPU_INFO_FAIL,
                params = arrayOf(queryResult.error)
            )
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        resultData[k_usage_privileged] = loadSerialData(queryResult.results[usage_privileged_idx], k_usage_privileged)
        resultData[k_usage_user] = loadSerialData(queryResult.results[usage_user_idx], k_usage_user)
        resultData[k_usage_interrupt] = loadSerialData(queryResult.results[usage_interrupt_idx], k_usage_interrupt)
        return resultData
    }
}
