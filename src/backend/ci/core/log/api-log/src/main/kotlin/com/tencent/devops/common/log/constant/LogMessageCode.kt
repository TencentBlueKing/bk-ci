package com.tencent.devops.common.log.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26:dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object LogMessageCode {
    const val ES_CLUSTER_ADDRESS_NOT_CONFIGURED = "2108001" // ES{0}集群地址尚未配置
    const val ES_CLUSTER_NAME_NOT_CONFIGURED = "2108002" // ES{0}集群名称尚未配置
    const val ES_UNIQUE_NAME_NOT_CONFIGURED = "2108003" // ES{0}唯一名称尚未配置
    const val USER_NO_RIGHT_VIEW_PIPELINE = "2108004" // 用户({0})无权限在工程({1})下查看流水线
    const val PRINT_QUEUE_LIMIT = "2108005" // log print queue exceeds the limit
    const val PRINT_IS_DISABLED = "2108006" // log print config is disabled
    const val FILE_NOT_FOUND_CHECK_PATH = "2108007" // 未找到 {0} 文件，请检查路径是否正确:
    const val LOG_INDEX_HAS_BEEN_CLEANED = "2108008" // 日志索引已被清理无法查看
    const val ERROR_PIPELINE_NOT_EXISTS = "2108009" // 流水线{0}不存在

    const val BK_FAILED_INSERT_DATA = "bkFailedInsertData" // 蓝盾ES集群插入数据失败
    const val BK_ES_CLUSTER_RECOVERY = "bkEsClusterRecovery" // 蓝盾ES集群恢复
    const val BK_FAILURE = "bkFailure" // 失效
    const val BK_RECOVERY = "bkRecovery" // 恢复
    const val BK_ES_CLUSTER_STATUS_ALARM_NOTIFICATION = "bkEsClusterStatusAlarmNotification" // 【ES集群状态告警通知】
    const val BK_NOTIFICATION_PUSH_FROM_BKDEVOPS = "bkNotificationPushFromBkdevops" // 来自bkDevOps/蓝盾DevOps平台的通知推送
    const val BK_CLUSTER_NAME = "bkClusterName" // 集群名称
    const val BK_STATUS = "bkStatus" // 状态
    const val BK_EMPTY_DATA = "bkEmptyData" // 空数据
    const val BK_LOOK_FORWARD_IT = "bkLookForwardIt" // 敬请期待！
    const val BK_CONTACT_BLUE_SHIELD_ASSISTANT = "bkContactBlueShieldAssistant" // 如有任何问题，可随时联系蓝盾助手。
    const val BK_HEAD_OF_BLUE_SHIELD_LOG_MANAGEMENT = "bkHeadOfBlueShieldLogManagement" // 你收到此邮件，是因为你是蓝盾日志管理负责人
}
