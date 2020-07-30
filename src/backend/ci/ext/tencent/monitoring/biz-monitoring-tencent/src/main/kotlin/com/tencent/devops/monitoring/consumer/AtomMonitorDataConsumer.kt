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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.monitoring.consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.web.mq.EXCHANGE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.QUEUE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.ROUTE_NOTIFY_MESSAGE
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.ERROR_MONITORING_INSERT_DATA_FAIL
import com.tencent.devops.monitoring.pojo.AtomMonitorData
import com.tencent.devops.monitoring.pojo.DispatchStatus
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.reflect.full.declaredMemberProperties

@Service
class AtomMonitorDataConsumer @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) {
    @RabbitListener(
        bindings = [(QueueBinding(
            key = ROUTE_NOTIFY_MESSAGE,
            value = Queue(value = QUEUE_NOTIFY_MESSAGE, durable = "true"),
            exchange = Exchange(value = EXCHANGE_NOTIFY_MESSAGE, durable = "true", delayed = "true", type = "topic")))
        ]
    )
    fun onReceiveAtomMonitorData(data: AtomMonitorData) {
        try {
            logger.info("Receive atom monitor data - $data")
            insertAtomMonitorData(data)
            if (data.atomCode == "CodeccCheckAtomDebug" && data.extData != null && data.extData!!.isNotEmpty()) {
                logger.info("CodeCC atom, insert extDAta - ${jacksonObjectMapper().writeValueAsString(data.extData)}")
                insertExtMap(data.extData!!)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to insert the atom monitor data", t)
            throw ErrorCodeException(
                errorCode = ERROR_MONITORING_INSERT_DATA_FAIL,
                defaultMessage = "Fail to insert the atom monitor data"
            )
        }
    }

    fun insertAtomMonitorData(data: AtomMonitorData) {
        val field: MutableMap<String, String> = mutableMapOf()
        val properties = data.javaClass.kotlin.declaredMemberProperties
        properties.forEach {
            field[it.name] = jacksonObjectMapper().writeValueAsString(it.get(data)?.toString() ?: "")
        }
        influxdbClient.insert(DispatchStatus::class.java.simpleName, emptyMap(), field)
    }

    fun insertExtMap(extData: Map<String, Any>) {
        val field: MutableMap<String, String> = mutableMapOf()
        extData.forEach { (t, u) ->
            field[t] = jacksonObjectMapper().writeValueAsString(u)
        }
        influxdbClient.insert(DispatchStatus::class.java.simpleName + "_extData", emptyMap(), field)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomMonitorDataConsumer::class.java)
    }
}