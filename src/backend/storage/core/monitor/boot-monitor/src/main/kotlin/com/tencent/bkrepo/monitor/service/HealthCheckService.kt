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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.monitor.service

import com.tencent.bkrepo.monitor.config.MonitorProperties
import com.tencent.bkrepo.monitor.metrics.HealthInfo
import com.tencent.bkrepo.monitor.notify.MessageNotifier
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class HealthCheckService(
    healthSourceService: HealthSourceService,
    messageNotifier: MessageNotifier,
    monitorProperties: MonitorProperties
) {
    val clusterName = monitorProperties.clusterName

    init {
        healthSourceService.getMergedSource()
            .filter { it.status.status != Status.UP }
            .flatMap { messageNotifier.notifyMessage(createContent(it)) }
            .doOnError { logger.error("Couldn't notify message.", it) }
            .subscribe()
    }

    private fun createContent(healthInfo: HealthInfo): Any {
        return with(healthInfo) {
            MESSAGE_TEMPLATE.format(application, instance, clusterName, name, status.status.code, status.details, LocalDateTime.now())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HealthCheckService::class.java)
        val MESSAGE_TEMPLATE =
            """
            <font color="warning">【提醒】</font>服务实例[%s-%s]健康检查失败
             > 集群: %s
             > 组件: %s
             > 状态: %s
             > 详情: %s
             > 时间: %s
            """.trimIndent()
    }
}
