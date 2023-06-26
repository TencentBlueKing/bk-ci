package com.tencent.devops.auth.constant

object AuthI18nConstants {
    const val RESOURCE_TYPE_NAME_SUFFIX = ".resourceType.name"
    const val RESOURCE_TYPE_DESC_SUFFIX = ".resourceType.desc"
    const val AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX = ".authResourceGroupConfig.groupName"
    const val AUTH_RESOURCE_GROUP_CONFIG_DESCRIPTION_SUFFIX = ".authResourceGroupConfig.description"
    const val ACTION_NAME_SUFFIX = ".actionName"
    const val BK_AGREE_RENEW = "bkAgreeRenew" // 同意续期
    const val BK_YOU_AGREE_RENEW = "bkYouAgreeRenew" // 你已选择同意用户续期
    const val BK_REFUSE_RENEW = "bkRefuseRenew" // 拒绝续期
    const val BK_YOU_REFUSE_RENEW = "bkYouRefuseRenew" // 你已选择拒绝用户续期
    // **蓝盾超级管理员权限续期申请审批**\n申请人：{0}\n授权名称：{1}\n授权详情：{2}\n用户权限过期时间：{3}\n请选择是否同意用户续期权限\n
    const val BK_WEWORK_ROBOT_NOTIFY_MESSAGE = "bkWeworkRobotNotifyMessage"
    const val BK_APPROVER_AGREE_RENEW = "bkApproverAgreeRenew" // 审批人同意了您的权限续期
    const val BK_APPROVER_REFUSE_RENEW = "bkApproverRefuseRenew" // 审批人拒绝了您的权限续期
    const val BK_ADMINISTRATOR_NOT_EXPIRED = "bkAdministratorNotExpired" // 权限还未过期，不需要操作！
    const val BK_AUTHORIZATION_SUCCEEDED = "bkAuthorizationSucceeded" // 授权成功, 获取管理员权限120分钟
    const val BK_CANCELLED_AUTHORIZATION_SUCCEEDED = "bkCancelledAuthorizationSucceeded"
    // 取消授权成功, 缓存在5分钟后完全失效
    const val BK_FAILED_CALL_CALLBACK_API = "bkFailedCallCallbackApi" // 调用回调接口失败
    const val BK_CREATE_BKCI_PROJECT_APPLICATION = "bkCreateBkciProjectApplication" // 创建蓝盾项目{0}申请
    const val BK_REVISE_BKCI_PROJECT_APPLICATION = "bkReviseBkciProjectApplication" // 修改蓝盾项目{0}申请
    const val BK_PROJECT_NAME = "bkProjectName" // 项目名称
    const val BK_PROJECT_ID = "bkProjectId" // 项目ID
    const val BK_PROJECT_DESC = "bkProjectDesc" // 项目描述
    const val BK_ORGANIZATION = "bkOrganization" // 所属组织
    const val BK_AUTH_SECRECY = "bkAuthSecrecy" // 项目性质
    const val BK_SUBJECT_SCOPES = "bkSubjectScopes" // 最大可授权人员范围
    const val BK_CREATE_PROJECT_APPROVAL = "bkCreateProjectApproval" // 创建项目{0}审批
}
