package com.tencent.devops.environment.client

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.environment.model.AgentHostInfo
import com.tencent.devops.environment.utils.NumberUtils
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Query
import org.influxdb.dto.QueryResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class InfluxdbClient {
    companion object {
        private val logger = LoggerFactory.getLogger(InfluxdbClient::class.java)
        private const val TIME_RANGE_HOUR = "HOUR"
        private const val TIME_RANGE_DAY = "DAY"
        private const val TIME_RANGE_WEEK = "WEEK"
        private const val DB = "agentMetrix"
        private const val TIME_PART_HOUR = "time >= now() - 1h and time <= now() - 30s GROUP BY time(10s)"
        private const val TIME_PART_DAY = "time >= now() - 24h and time <= now() - 30s GROUP BY time(2m)"
        private const val TIME_PART_WEEK = "time >= now() - 7d and time <= now() - 30s GROUP BY time(10m)"
        private const val TIME_GROUP_BY_HOUR = "10s"
        private const val TIME_GROUP_BY_DAY = "2m"
        private const val TIME_GROUP_BY_WEEK = "10m"
    }

    @Value("\${influxdb.server:}")
    val influxdbServer: String = ""

    @Value("\${influxdb.userName:}")
    val influxdbUserName: String = ""

    @Value("\${influxdb.password:}")
    val influxdbPassword: String = ""

    private fun getInfluxDb(): InfluxDB? {
        logger.info("getInfluxDb -> influxdbServer: $influxdbServer")
        return try {
            InfluxDBFactory.connect(influxdbServer, influxdbUserName, influxdbPassword)
        } catch (ignored: Exception) {
            logger.error("getInfluxDb| fail, msg=${ignored.message}", ignored)
            null // 返回空，防止页面异常。 顶多是数量展示空。 需要用户根据日志定位问题
        }
    }

    fun queryHostInfo(agentHashId: String): AgentHostInfo {
        val queryStr =
            "SELECT last(\"n_cpus\") FROM \"system\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND time >= now() - 7d and time <= now() - 30s; SELECT last(\"total\") FROM \"mem\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND time >= now() - 7d and time <= now() - 30s; SELECT max(\"total\") FROM \"disk\" WHERE \"agentId\" =~ /^$agentHashId\$/ and time >= now() - 7d and time <= now() - 30s"
        logger.info("queryStr: $queryStr")

        var nCpus = "0"
        var memTotal = "0"
        var diskTotal = "0"

        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return AgentHostInfo(nCpus, memTotal, diskTotal)
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

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
        } catch (e: Exception) {
            logger.warn("parse agent host info failed", e)
        }

        return AgentHostInfo(nCpus, memTotal, diskTotal)
    }

    private fun buildSerieData(result: QueryResult.Result, serieName: String): List<Map<String, Any>> {
        if (result.hasError()) {
            logger.warn("result error: ", result.error)
            return listOf()
        }
        return if (result.series == null || result.series.isEmpty()) {
            listOf()
        } else {
            result.series[0].values.map {
                mapOf(
                    "time" to it[0],
                    serieName to it[1]
                )
            }
        }
    }

    private fun getTimePart(timeRange: String): String {
        return when (timeRange) {
            TIME_RANGE_HOUR -> TIME_PART_HOUR
            TIME_RANGE_WEEK -> TIME_PART_WEEK
            else -> TIME_PART_DAY
        }
    }

    private fun getTimeGroupBy(timeRange: String): String {
        return when (timeRange) {
            TIME_RANGE_HOUR -> TIME_GROUP_BY_HOUR
            TIME_RANGE_WEEK -> TIME_GROUP_BY_WEEK
            else -> TIME_GROUP_BY_DAY
        }
    }

    fun queryLinuxCpuUsageMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT mean(\"usage_idle\") FROM \"cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null); SELECT mean(\"usage_iowait\") FROM \"cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null); SELECT mean(\"usage_user\") FROM \"cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null); SELECT mean(\"usage_system\") FROM \"cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf(
                "usage_idle" to listOf(mapOf("time" to "0")),
                "usage_iowait" to listOf(mapOf("time" to "0")),
                "usage_user" to listOf(mapOf("time" to "0")),
                "usage_system" to listOf(mapOf("time" to "0"))
            )
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        resultData["usage_idle"] = buildSerieData(queryResult.results[0], "usage_idle")
        resultData["usage_iowait"] = buildSerieData(queryResult.results[1], "usage_iowait")
        resultData["usage_user"] = buildSerieData(queryResult.results[2], "usage_user")
        resultData["usage_system"] = buildSerieData(queryResult.results[3], "usage_system")
        return resultData
    }

    fun queryWindowsCpuUsageMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT mean(\"Percent_Privileged_Time\") FROM \"win_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null); SELECT mean(\"Percent_User_Time\") FROM \"win_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null); SELECT mean(\"Percent_Interrupt_Time\") FROM \"win_cpu\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf(
                "usage_privileged" to listOf(mapOf("time" to "0")),
                "usage_user" to listOf(mapOf("time" to "0")),
                "usage_interrupt" to listOf(mapOf("time" to "0"))
            )
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        resultData["usage_privileged"] = buildSerieData(queryResult.results[0], "usage_privileged")
        resultData["usage_user"] = buildSerieData(queryResult.results[1], "usage_user")
        resultData["usage_interrupt"] = buildSerieData(queryResult.results[2], "usage_interrupt")
        return resultData
    }

    fun queryMemoryUsageMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val queryStr =
            "SELECT mean(\"used_percent\") FROM \"mem\" WHERE \"agentId\" =~ /^$agentHashId\$/ and ${getTimePart(
                timeRange
            )} fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf("used_percent" to listOf(mapOf("time" to "0")))
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        resultData["used_percent"] = buildSerieData(queryResult.results[0], "used_percent")
        return resultData
    }

    fun queryLinuxDiskioMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timeGroupBy = getTimeGroupBy(timeRange)
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT non_negative_derivative(mean(\"read_bytes\"), $timeGroupBy) as \"read\" FROM \"diskio\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart, \"name\" fill(null); SELECT non_negative_derivative(mean(\"write_bytes\"), $timeGroupBy)  as \"write\" FROM \"diskio\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart, \"name\" fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf("dev" to listOf(mapOf("time" to "0")))
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        queryResult.results.forEach { qResult ->
            if (qResult.hasError()) {
                logger.warn("result error: ", qResult.error)
                return@forEach
            }
            if (qResult.series == null || qResult.series.isEmpty()) {
                return@forEach
            }
            qResult.series.forEach { qSerie ->
                val serieName = "${qSerie.tags["name"]}:${qSerie.columns[1]}"
                resultData[serieName] = qSerie.values.map {
                    mapOf(
                        "time" to it[0],
                        serieName to it[1]
                    )
                }
            }
        }
        return resultData
    }

    fun queryWindowsDiskioMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timeGroupBy = getTimeGroupBy(timeRange)
        val timePart = getTimePart(timeRange)
        val queryStr =
            "select mean(\"Disk_Write_Bytes_persec\") as \"write\" from \"win_diskio\" where \"agentId\" =~ /$agentHashId\$/ and $timePart, \"instance\" fill(null); select mean(\"Disk_Read_Bytes_persec\") as \"read\"  from \"win_diskio\" where \"agentId\" =~ /$agentHashId\$/ and $timePart, \"instance\" fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf("io" to listOf(mapOf("time" to "0")))
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        queryResult.results.forEach { qResult ->
            if (qResult.hasError()) {
                logger.warn("result error: ", qResult.error)
                return@forEach
            }
            if (qResult.series == null || qResult.series.isEmpty()) {
                return@forEach
            }
            qResult.series.forEach { qSerie ->
                val serieName = "${qSerie.tags["instance"]}:${qSerie.columns[1]}"
                resultData[serieName] = qSerie.values.map {
                    mapOf(
                        "time" to it[0],
                        serieName to it[1]
                    )
                }
            }
        }
        return resultData
    }

    fun queryLinuxNetMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timeGroupBy = getTimeGroupBy(timeRange)
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT non_negative_derivative(mean(\"bytes_recv\"), $timeGroupBy) as \"IN\" FROM \"net\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart, \"interface\" fill(null); SELECT non_negative_derivative(mean(\"bytes_sent\"), $timeGroupBy)  as \"OUT\" FROM \"net\" WHERE \"agentId\" =~ /^$agentHashId\$/ AND $timePart, \"interface\" fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf("eth0" to listOf(mapOf("time" to "0")))
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        queryResult.results.forEach { qResult ->
            if (qResult.hasError()) {
                logger.warn("result error: ", qResult.error)
                return@forEach
            }
            if (qResult.series == null || qResult.series.isEmpty()) {
                return@forEach
            }
            qResult.series.forEach { qSerie ->
                val serieName = "${qSerie.tags["interface"]}:${qSerie.columns[1]}"
                resultData[serieName] = qSerie.values.map {
                    mapOf(
                        "time" to it[0],
                        serieName to it[1]
                    )
                }
            }
        }
        return resultData
    }

    fun queryWindowsNetMetrix(agentHashId: String, timeRange: String): Map<String, List<Map<String, Any>>> {
        val timeGroupBy = getTimeGroupBy(timeRange)
        val timePart = getTimePart(timeRange)
        val queryStr =
            "SELECT non_negative_derivative(mean(\"Bytes_Received_persec\"), $timeGroupBy) as \"received\" FROM \"win_net\" WHERE \"agentId\" =~ /$agentHashId\$/ AND $timePart, \"instance\" fill(null); SELECT non_negative_derivative(mean(\"Bytes_Sent_persec\"), $timeGroupBy) as \"sent\" FROM \"win_net\" WHERE \"agentId\" =~ /$agentHashId\$/ AND $timePart, \"instance\" fill(null)"
        logger.info("queryStr: $queryStr")
        val queryResult = getInfluxDb()?.query(Query(queryStr, DB))
            ?: return mapOf("net" to listOf(mapOf("time" to "0")))
        if (queryResult.hasError()) {
            logger.error("query influxdb error: ", queryResult.error)
            throw OperationException("query influxdb failed")
        }

        val resultData = mutableMapOf<String, List<Map<String, Any>>>()
        queryResult.results.forEach { qResult ->
            if (qResult.hasError()) {
                logger.warn("result error: ", qResult.error)
                return@forEach
            }
            if (qResult.series == null || qResult.series.isEmpty()) {
                return@forEach
            }
            qResult.series.forEach { qSerie ->
                val serieName = "${qSerie.tags["instance"]}:${qSerie.columns[1]}"
                resultData[serieName] = qSerie.values.map {
                    mapOf(
                        "time" to it[0],
                        serieName to it[1]
                    )
                }
            }
        }
        return resultData
    }
}