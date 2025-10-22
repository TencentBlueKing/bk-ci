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

package com.tencent.devops.monitoring.consumer

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.AtomMonitorData
import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.common.event.pojo.measure.AtomMonitorReportBroadCastEvent
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.ERROR_MONITORING_INSERT_DATA_FAIL
import com.tencent.devops.monitoring.consumer.processor.monitor.AbstractMonitorProcessor
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AtomMonitorReportListener @Autowired constructor(
    private val influxdbClient: InfluxdbClient,
    private val monitorProcessors: List<AbstractMonitorProcessor>,
    private val meterRegistry: MeterRegistry
) : EventListener<AtomMonitorReportBroadCastEvent> {

    override fun execute(event: AtomMonitorReportBroadCastEvent) {
        try {
            val monitorData = event.monitorData
            logger.info("Receive monitorData - $monitorData")
            insertAtomMonitorData(monitorData)

            monitorProcessors.asSequence().filter { it.atomCode() == monitorData.atomCode }
                .forEach { it.process(influxdbClient, monitorData) }
        } catch (ignored: Throwable) {
            logger.warn("Fail to insert the atom monitor data", ignored)
            throw ErrorCodeException(
                errorCode = ERROR_MONITORING_INSERT_DATA_FAIL,
                defaultMessage = "Fail to insert the atom monitor data"
            )
        }
    }

    fun insertAtomMonitorData(data: AtomMonitorData) {
        // 写入influxdb
        influxdbClient.insert(data)

        // 暴露prometheus
        Counter.builder("atom_monitor")
            .tag("atomCode", data.atomCode)
            .tag("errorCode", data.errorCode.toString())
            .tag("errorType", data.errorType ?: "null")
            .register(meterRegistry)
            .increment()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomMonitorReportListener::class.java)
    }
}
