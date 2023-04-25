package com.tencent.devops.auth.constant

object AuthI18nConstants {
    const val BK_AGREE_RENEW = "bkAgreeRenew"// 同意续期
    const val BK_YOU_AGREE_RENEW = "bkYouAgreeRenew"// 你已选择同意用户续期
    const val BK_REFUSE_RENEW = "bkRefuseRenew"// 拒绝续期
    const val BK_YOU_REFUSE_RENEW = "bkYouRefuseRenew"// 你已选择拒绝用户续期
    const val BK_WEWORK_ROBOT_NOTIFY_MESSAGE = "bkWeworkRobotNotifyMessage"// 微信机器人通知
    const val BK_APPROVER_AGREE_RENEW = "bkApproverAgreeRenew"// 审批人同意了您的权限续期
    const val BK_APPROVER_REFUSE_RENEW = "bkApproverRefuseRenew"// 审批人拒绝了您的权限续期
    const val BK_ADMINISTRATOR_NOT_EXPIRED = "bkAdministratorNotExpired"// 权限还未过期，不需要操作！
    const val BK_AUTHORIZATION_SUCCEEDED = "bkAuthorizationSucceeded"// 授权成功, 获取管理员权限120分钟
    const val BK_CANCELLED_AUTHORIZATION_SUCCEEDED = "bkCancelledAuthorizationSucceeded"// 取消授权成功, 缓存在5分钟后完全失效
    const val BK_FAILED_CALL_CALLBACK_API = "bkFailedCallCallbackApi"// 调用回调接口失败
}