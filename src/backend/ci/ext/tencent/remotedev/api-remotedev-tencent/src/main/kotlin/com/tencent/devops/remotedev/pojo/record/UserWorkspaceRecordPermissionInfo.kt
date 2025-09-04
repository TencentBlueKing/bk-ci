package com.tencent.devops.remotedev.pojo.record

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户查看工作空间录屏权限信息")
data class UserWorkspaceRecordPermissionInfo(
    @get:Schema(title = "是否开启录屏", required = true)
    val enableRecord: Boolean,
    @get:Schema(title = "是否有权限查看", required = true)
    val viewPermission: Boolean,
    @get:Schema(title = "有权限查看时，权限的截止时间", required = true)
    val viewPermissionEndTime: Long?
)