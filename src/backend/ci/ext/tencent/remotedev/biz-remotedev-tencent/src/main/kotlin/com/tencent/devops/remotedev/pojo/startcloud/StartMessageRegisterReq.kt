package com.tencent.devops.remotedev.pojo.startcloud

data class StartMessageRegisterReq(
    val operator: String,
    val orderId: String,
    val user: StartMessageRegisterUserStrategy,
    val condition: StartMessageRegisterCondition,
    val data: String
)

/**
 * 如果填写多个字段则取交集， 以最小规则下发
 */
data class StartMessageRegisterUserStrategy(
    val userIds: Set<String>?,
    val contentProvideName: String?,
    val appName: String?
)

/**
 * @param type StartMessageType
 */
data class StartMessageRegisterCondition(
    val type: Int,
    val startTime: Long,
    val endTime: Long
)

enum class StartMessageType(val value: Int) {
    // 及时消息
    INSTANT(1)
}

/**
 * @param type StartMessageDataType
 * @param content base64编码
 */
data class StartMessageRegisterData(
    val type: Int,
    val content: String
)
