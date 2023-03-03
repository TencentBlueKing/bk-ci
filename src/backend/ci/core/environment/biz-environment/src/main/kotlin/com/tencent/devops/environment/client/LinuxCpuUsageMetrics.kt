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
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_cpu_usage_idle
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_cpu_usage_iowait
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_cpu_usage_system
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.f_cpu_usage_user
import com.tencent.devops.environment.client.AgentMetricsTargetConstant.t_cpu
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import org.influxdb.dto.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@AgentMetrics(metricsType = UsageMetrics.MetricsType.CPU, osTypes = [OS.LINUX, OS.MACOS])
class LinuxCpuUsageMetrics @Autowired constructor(val influxdbClient: InfluxdbClient) : UsageMetrics {

    companion object {
        private const val usage_idle_idx = 0
        private const val usage_iowait_idx = 1
        private const val usage_user_idx = 2
        private const val usage_system_idx = 3

        private val emptyCpuMetrics = mapOf(
            f_cpu_usage_idle to UsageMetrics.emptyMetrics,
            f_cpu_usage_iowait to UsageMetrics.emptyMetrics,
            f_cpu_usage_user to UsageMetrics.emptyMetrics,
            f_cpu_usage_system to UsageMetrics.emptyMetrics
        )
    }

    @PostConstruct
    private fun init() {
        UsageMetrics.registerBean(this)
    }

    override fun loadQuery(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT mean(\"$f_cpu_usage_idle\") FROM \"$t_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/" +
                    " AND $timePart fill(null); " +
                    "SELECT mean(\"$f_cpu_usage_iowait\") FROM \"$t_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                    "AND $timePart fill(null); " +
                    "SELECT mean(\"$f_cpu_usage_user\") FROM \"$t_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ " +
                    "AND $timePart fill(null); " +
                    "SELECT mean(\"$f_cpu_usage_system\") FROM \"$t_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/" +
                    " AND $timePart fill(null)"

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
        resultData[f_cpu_usage_idle] = loadSerialData(queryResult.results[usage_idle_idx], f_cpu_usage_idle)
        resultData[f_cpu_usage_iowait] = loadSerialData(queryResult.results[usage_iowait_idx], f_cpu_usage_iowait)
        resultData[f_cpu_usage_user] = loadSerialData(queryResult.results[usage_user_idx], f_cpu_usage_user)
        resultData[f_cpu_usage_system] = loadSerialData(queryResult.results[usage_system_idx], f_cpu_usage_system)
        return resultData
    }
}
