package com.tencent.devops.remotedev.pojo

data class OPUserSetting(
    val userId: String,
    val wsMaxRunningCount: Int?,
    val wsMaxHavingCount: Int?,
    val grayFlag: Boolean?
)
