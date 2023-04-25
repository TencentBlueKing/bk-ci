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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.monitoring.pojo.GrafanaMessage
import com.tencent.devops.monitoring.pojo.GrafanaNotification
import com.tencent.devops.monitoring.pojo.NocNoticeBusData
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@RefreshScope
class GrafanaWebhookService @Autowired constructor(
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(GrafanaWebhookService::class.java)

    @Value("\${alert.users}")
    private val alertUsersStr: String = "irwinsun"

    private lateinit var alertUsers: Set<String>

    @PostConstruct
    fun init() {
        alertUsers = alertUsersStr.split(",").toSet()
        logger.info("alert User= $alertUsers")
    }

    /**
     * grafana回调接口
     */
    fun webhookCallBack(grafanaNotification: GrafanaNotification): Result<Boolean> {
        logger.info("webhookCallBack grafanaNotification is:$grafanaNotification")
        // 只有处于alerting告警状态的信息才发送监控消息
        if ("Alerting".equals(grafanaNotification.state, true)) {
            val message = grafanaNotification.message
            val grafanaMessage = try {
                JsonUtil.to(message, GrafanaMessage::class.java)
            } catch (e: Exception) {
                JsonUtil.to(message, SendNotifyMessageTemplateRequest::class.java)
            }
            val sendMessage = when (grafanaMessage) {
                is SendNotifyMessageTemplateRequest -> {
                    grafanaMessage
                }
                is GrafanaMessage -> {
                    SendNotifyMessageTemplateRequest(
                        templateCode = "GRAFANA_ALERTING",
                        receivers = grafanaMessage.notifyReceivers ?: mutableSetOf(),
                        notifyType = mutableSetOf(grafanaMessage.notifyType?.name ?: "RTX"),
                        titleParams = mapOf(),
                        bodyParams = mapOf("data" to grafanaMessage.notifyMessage, "url" to "来自Grafana的预警信息")
                    )
                }
                else -> return Result(data = false)
            }
            val notifyMessage = (sendMessage.bodyParams ?: emptyMap()).toMutableMap()
            val evalMatches = grafanaNotification.evalMatches
            val busiDataList = mutableListOf<NocNoticeBusData>()
            if (null != evalMatches && evalMatches.isNotEmpty()) {
                notifyMessage["data"] += "（"
                evalMatches.forEach {
                    val metricName = it.metric
                    val metricValue = it.value
                    notifyMessage["data"] += " 监控对象：$metricName，当前值为：$metricValue；"
                    busiDataList.add(NocNoticeBusData(metricName, metricValue))
                }
                notifyMessage["data"] += "）"
            }
            return client.get(ServiceNotifyMessageTemplateResource::class)
                .sendNotifyMessageByTemplate(sendMessage.copy(bodyParams = notifyMessage))
        }
        return Result(data = false, message = "只有处于alerting告警状态的信息才发送监控消息")
    }
}
