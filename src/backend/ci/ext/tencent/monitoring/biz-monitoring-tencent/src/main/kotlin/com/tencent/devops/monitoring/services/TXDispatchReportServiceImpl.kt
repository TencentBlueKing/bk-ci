package com.tencent.devops.monitoring.services

import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.pojo.DispatchStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TXDispatchReportServiceImpl @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) {
    private val logger = LoggerFactory.getLogger(TXDispatchReportServiceImpl::class.java)

    fun reportDispatchStatus(dispatchStatus: DispatchStatus): Boolean {
        return try {
            influxdbClient.insert(dispatchStatus)
            true
        } catch (e: Throwable) {
            logger.error("reportDispatchStatus exception:", e)
            false
        }
    }
}