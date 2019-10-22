package com.tencent.devops.plugin.pojo.wetest

data class WetestEmailGroupParam(
    val name: String,
    val userInternal: String,
    val qqExternal: String?,
    val description: String?,
    val wetestGroupId: String?,
    val wetestGroupName: String?
)
