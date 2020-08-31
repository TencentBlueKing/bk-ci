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
            logger.error("get baseQueryResult error , ${baseQueryResult?.error} , ${baseQueryResult?.results?.size}")
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
            logger.error("get errorQueryResult error , ${errorQueryResult?.error} , ${errorQueryResult?.results?.size}")
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