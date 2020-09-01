package com.tencent.devops.monitoring.services

import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.pojo.DispatchStatus
import com.tencent.devops.monitoring.service.DispatchReportService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class TXDispatchReportServiceImpl @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) : DispatchReportService {
    private val logger = LoggerFactory.getLogger(TXDispatchReportServiceImpl::class.java)

    override fun reportDispatchStatus(dispatchStatus: DispatchStatus): Boolean {
        return try {
            influxdbClient.insert(dispatchStatus)
            true
        } catch (e: Throwable) {
            logger.error("reportDispatchStatus exception:", e)
            false
        }
    }
}