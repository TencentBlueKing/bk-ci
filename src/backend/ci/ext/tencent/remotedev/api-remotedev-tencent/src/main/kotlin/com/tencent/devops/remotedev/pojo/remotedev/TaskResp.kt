package com.tencent.devops.remotedev.pojo.remotedev

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "发起任务返回")
data class TaskResp(
    @get:Schema(title = "发起任务是否成功")
    val result: Boolean,
    @get:Schema(title = "发起成功后任务ID")
    val taskId: String?,
    @get:Schema(title = "发起失败后可能的失败原因")
    val errMsg: String?
)

/**
 * 后台调用时使用，不在前端返回
 */
data class TaskCommonResp(
    val taskId: String,
    val taskUid: String
)