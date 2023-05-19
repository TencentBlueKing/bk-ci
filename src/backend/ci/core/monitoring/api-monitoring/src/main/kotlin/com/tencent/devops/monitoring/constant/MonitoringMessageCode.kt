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

package com.tencent.devops.monitoring.constant

object MonitoringMessageCode {
    const val ERROR_MONITORING_SEND_NOTIFY_FAIL = "2110008" // 监控服务：通知发送失败
    const val ERROR_MONITORING_INSERT_DATA_FAIL = "2110009" // 监控服务：写入influxdb失败
    const val ERROR_MONITORING_INFLUXDB_BAD = "2110010" // 监控服务：influxdb异常

    const val BK_ILLEGAL_TIMESTAMP_RANGE = "bkIllegalTimestampRange" // 非法时间戳范围
    const val BK_ILLEGAL_ENTERPRISE_GROUP_ID = "bkIllegalEnterpriseGroupId" // 非法事业群ID
    const val BK_INCORRECT_PASSWORD = "bkIncorrectPassword" // 密码错误
    const val BK_SENT_SUCCESSFULLY = "bkSentSuccessfully" // 发送成功
    const val BK_WARNING_MESSAGE_FROM_GRAFANA = "bkWarningMessageFromGrafana" // 来自Grafana的预警信息
    const val BK_MONITORING_OBJECT = "bkMonitoringObject" // 监控对象：{0}，当前值为：{1}；
    const val BK_SEND_MONITORING_MESSAGES = "bkSendMonitoringMessages" // 只有处于alerting告警状态的信息才发送监控消息
}
