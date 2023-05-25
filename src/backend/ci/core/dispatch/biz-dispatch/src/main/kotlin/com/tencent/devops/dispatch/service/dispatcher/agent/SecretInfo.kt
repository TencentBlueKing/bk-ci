package com.tencent.devops.dispatch.service.dispatcher.agent

/** issue_7748 搬用 dispatch sdk 的类，因为sdk集成当前存在问题
 *  @see com.tencent.devops.common.dispatch.sdk.pojo.SecretInfo
 **/
data class SecretInfo(
    val hashId: String,
    val secretKey: String
)
