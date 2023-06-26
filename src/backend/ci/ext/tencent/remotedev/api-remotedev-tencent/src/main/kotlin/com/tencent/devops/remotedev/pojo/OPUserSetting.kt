package com.tencent.devops.remotedev.pojo

data class OPUserSetting(
    val userId: String,
    val maxRunningCount: Int?,
    val maxHavingCount: Int?,
    val onlyCloudIDE: Boolean?,
    val grayFlag: Boolean?,
    val allowedCopy: Boolean?,
    val allowedDownload: Boolean?,
    val needWatermark: Boolean?,
    val autoDeletedDays: Int?,
    val mountType: WorkspaceMountType?
)
