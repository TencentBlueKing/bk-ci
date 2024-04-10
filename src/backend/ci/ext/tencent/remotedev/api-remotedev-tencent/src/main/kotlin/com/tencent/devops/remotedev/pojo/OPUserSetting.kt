package com.tencent.devops.remotedev.pojo

data class OPUserSetting(
    val userIds: List<String>,
    val maxRunningCount: Int?,
    val maxHavingCount: Int?,
    val onlyCloudIDE: Boolean?,
    val grayFlag: Boolean?,
    val startCloudExperienceDuration: Int?,
    val allowedCopy: Boolean?,
    val allowedDownload: Boolean?,
    val needWatermark: Boolean?,
    val autoDeletedDays: Int?,
    val mountType: WorkspaceMountType?,
    val clientWhiteList: Boolean?,
    val startWhiteList: Boolean?
)
