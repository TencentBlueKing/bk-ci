package com.tencent.devops.remotedev.pojo

data class OPUserSetting(
    val userId: String,
    val wsMaxRunningCount: Int?,
    val wsMaxHavingCount: Int?,
    val onlyCloudIDE: Boolean?,
    val grayFlag: Boolean?,
    val startCloudExperienceDuration: Int?,
    val allowedCopy: Boolean?,
    val allowedDownload: Boolean?,
    val needWatermark: Boolean?,
    val autoDeletedDays: Int?
)
