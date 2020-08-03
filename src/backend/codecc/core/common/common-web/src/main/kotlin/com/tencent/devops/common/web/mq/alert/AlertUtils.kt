/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.devops.common.web.mq.alert

import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.mq.EXCHANGE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.ROUTE_NOTIFY_MESSAGE
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

object AlertUtils {

    fun doAlert(level: AlertLevel, title: String, message: String) {
        val serviceName = SpringContextUtil.getBean(Profile::class.java).getApplicationName() ?: ""
        doAlert(serviceName, level, title, message)
    }

    fun doAlert(module: String, level: AlertLevel, title: String, message: String) {
        try {
            val alert = Alert(module, level, title, message)
            logger.info("Start to send the notify $alert")
            val rabbitTemplate = SpringContextUtil.getBean(RabbitTemplate::class.java)
            rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY_MESSAGE, ROUTE_NOTIFY_MESSAGE, alert)
        } catch (t: Throwable) {
            logger.warn("Fail to send the notify alert (level=$level, title=$title, message=$message)", t)
        }
    }

    private val logger = LoggerFactory.getLogger(AlertUtils::class.java)
}