package com.tencent.devops.monitoring.constant

object MonitoringCode {
    const val BK_ILLEGAL_TIMESTAMP_RANGE = "BkIllegalTimestampRange" //非法时间戳范围
    const val BK_ILLEGAL_ENTERPRISE_GROUP_ID = "BkIllegalEnterpriseGroupId" //非法事业群ID
    const val BK_INCORRECT_PASSWORD = "BkIncorrectPassword" //密码错误
    const val BK_SENT_SUCCESSFULLY = "BkSentSuccessfully" //发送成功
    const val BK_WARNING_MESSAGE_FROM_GRAFANA = "BkWarningMessageFromGrafana" //来自Grafana的预警信息
    const val BK_MONITORING_OBJEC = "BkMonitoringObjec" //监控对象：{0}，当前值为：{1}；
    const val BK_SEND_MONITORING_MESSAGES = "BkSendMonitoringMessages" //只有处于alerting告警状态的信息才发送监控消息
}