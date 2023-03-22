package com.tencent.devops.log

object LogCode {
    const val BK_FAILED_INSERT_DATA = "BkFailedInsertData"//蓝盾ES集群插入数据失败
    const val BK_ES_CLUSTER_RECOVERY = "BkEsClusterRecovery"//蓝盾ES集群恢复
    const val BK_FAILURE = "BkFailure"//失效
    const val BK_RECOVERY = "BkRecovery"//恢复
    const val BK_ES_CLUSTER_STATUS_ALARM_NOTIFICATION = "BkEsClusterStatusAlarmNotification"//【ES集群状态告警通知】
    const val BK_NOTIFICATION_PUSH_FROM_BKDEVOP = "BkNotificationPushFromBkdevop"//来自BKDevOps/蓝盾DevOps平台的通知推送
    const val BK_CLUSTER_NAME = "BkClusterName"//集群名称
    const val BK_STATUS = "BkStatus"//状态
    const val BK_EMPTY_DATA = "BkEmptyData"//空数据
    const val BK_LOOK_FORWARD_IT = "BkLookForwardIt"//敬请期待！
    const val BK_CONTACT_BLUE_SHIELD_ASSISTANT = "BkContactBlueShieldAssistant"//如有任何问题，可随时联系蓝盾助手。
    const val BK_HEAD_OF_BLUE_SHIELD_LOG_MANAGEMENT  = "BkHeadOfBlueShieldLogManagement"//你收到此邮件，是因为你是蓝盾日志管理负责人

    const val BK_ES_CLUSTER_ADDRESS_NOT_CONFIGURED = "BkEsClusterAddressNotConfigured"//ES{0}集群地址尚未配置
    const val BK_ES_CLUSTER_NAME_NOT_CONFIGURED = "BkEsClusterNameNotConfigured"//ES{0}集群名称尚未配置
    const val BK_ES_UNIQUE_NAME_NOT_CONFIGURED = "BkEsUniqueNameNotConfigured"//ES{0}唯一名称尚未配置
    const val BK_USER_NO_RIGHT_VIEW_PIPELINE = "BkUserNoRightViewPipeline"//用户({0})无权限在工程({1})下查看流水线


}