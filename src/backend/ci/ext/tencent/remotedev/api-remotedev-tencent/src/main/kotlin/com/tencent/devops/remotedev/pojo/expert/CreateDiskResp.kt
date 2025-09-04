package com.tencent.devops.remotedev.pojo.expert

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "磁盘创建任务详情")
data class CreateDiskResp(
    @get:Schema(title = "创建任务发起是否成功")
    val result: Boolean,
    @get:Schema(title = "创建发起不成功原因")
    val message: String?,
    @get:Schema(title = "任务流ID")
    val taskId: String?
)

@Schema(title = "删除磁盘详情")
data class DeleteDiskData(
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "数据盘唯一名称")
    val pvcName: String,
    @get:Schema(title = "是否强制重启")
    val forceRestart: Boolean?,
    @get:Schema(title = "延迟删除时间，秒")
    val delaySeconds: Int?
)

enum class CreateDiskDataClass(val data: String) {
    SSD("ssd"),
    HDD("hdd")
}
