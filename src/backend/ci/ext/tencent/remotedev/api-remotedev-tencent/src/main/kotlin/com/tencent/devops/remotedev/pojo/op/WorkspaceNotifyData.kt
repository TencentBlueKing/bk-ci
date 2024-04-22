package com.tencent.devops.remotedev.pojo.op

import com.tencent.devops.remotedev.pojo.start.StartMessageDataType
import io.swagger.v3.oas.annotations.Parameter

data class WorkspaceNotifyData(
    @Parameter(description = "projectId", required = true)
    val projectId: List<String>?,
    @Parameter(description = "ip", required = false)
    val ip: List<String>?,
    @Parameter(description = "owner", required = false)
    val owner: List<String>?,
    @Parameter(description = "title", required = true)
    val title: String,
    @Parameter(description = "desc", required = false)
    val desc: String?
)

data class WorkspaceDesktopNotifyData(
    @Parameter(description = "操作人", required = true)
    val operator: String,
    @Parameter(description = "需要发送的用户", required = true)
    val userIdList: Set<String>,
    @Parameter(description = "发送消息的数据类型", required = true)
    val dataType: StartMessageDataType,
    @Parameter(description = "发送的消息，需要base64编码", required = true)
    val data: String,
    @Parameter(description = "消息开始时间", required = true)
    val messageStartTime: Long,
    @Parameter(description = "消息结束时间", required = true)
    val messageEndTime: Long
)
