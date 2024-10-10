package com.tencent.devops.remotedev.pojo.remotedev

/**
 * bcs返回数据总包装
 */
data class BcsResp<out T>(
    val result: Boolean,
    val code: Int,
    val message: String?,
    val data: T?
)

data class BcsTaskData(
    val taskUid: String
)

data class BcsTaskDataV2(
    val taskUid: String,
    val taskID: String?
)