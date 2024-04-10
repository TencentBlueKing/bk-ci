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

package com.tencent.devops.monitoring.services

import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.constant.SlaPluginError
import com.tencent.devops.monitoring.pojo.ErrorPie
import com.tencent.devops.monitoring.pojo.SlaCodeccResponseData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SlaMonitorService @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) {
    fun codeccQuery(bgId: String, startTime: Long, endTime: Long): SlaCodeccResponseData {
        logger.info("codeccQuery , bgId:$bgId , startTime:$startTime , endTime:$endTime")

        var totalCount = 0
        var costTime = 0L
        val errorPie = mutableListOf<ErrorPie>()

        val baseSql =
            "SELECT sum(total_count) , mean(avg_time) FROM CodeccMonitor_reduce WHERE time>${startTime}000000 AND time<${endTime}000000 AND bgId='$bgId'"
        val baseQueryResult = influxdbClient.select(baseSql)
        if (null != baseQueryResult && !baseQueryResult.hasError()) {
            baseQueryResult.results.forEach { result ->
                result.run {
                    series?.forEach { serie ->
                        serie.run {
                            totalCount = values[0][1].let { if (it is Number) it.toInt() else 0 }
                            costTime = values[0][2].let { if (it is Number) it.toLong() else 0 }
                        }
                    }
                }
            }
        } else {
            logger.warn("get baseQueryResult error , ${baseQueryResult?.error} , ${baseQueryResult?.results?.size}")
            return SlaCodeccResponseData.EMPTY
        }

        val errorSql =
            "SELECT sum(total_count) FROM CodeccMonitor_reduce WHERE errorCode!='0' AND time>${startTime}000000 AND time<${endTime}000000 AND bgId='$bgId' GROUP BY errorCode"
        val errorQueryResult = influxdbClient.select(errorSql)
        if (null != errorQueryResult && !errorQueryResult.hasError()) {
            errorQueryResult.results.forEach { result ->
                result.run {
                    series?.forEach { serie ->
                        serie.run {
                            errorPie.add(ErrorPie(
                                tags["errorCode"],
                                SlaPluginError.getMean(tags["errorCode"]),
                                values[0][1].let { if (it is Number) it.toInt() else 0 }
                            ))
                        }
                    }
                }
            }
        } else {
            logger.warn("get errorQueryResult error , ${errorQueryResult?.error} , ${errorQueryResult?.results?.size}")
            return SlaCodeccResponseData.EMPTY
        }

        return SlaCodeccResponseData(
            totalCount,
            costTime,
            1 - (errorPie.asSequence().map { it.count }.sum().toDouble() / totalCount),
            errorPie
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlaMonitorService::class.java)
    }
}
